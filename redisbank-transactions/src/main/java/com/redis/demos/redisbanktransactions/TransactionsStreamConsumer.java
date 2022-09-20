package com.redis.demos.redisbanktransactions;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RediSearchCommands;
import com.redis.lettucemod.search.CreateOptions;
import com.redis.lettucemod.search.CreateOptions.DataType;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.TextField.PhoneticMatcher;

import io.lettuce.core.RedisCommandExecutionException;

@Component
public class TransactionsStreamConsumer
        implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {

    private static final String SEARCH_INDEX = "transactions_idx";

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsStreamConsumer.class);
    private static final String TRANSACTIONS_STREAM = "transactions";

    private final StringRedisTemplate transactionsRedis;
    private final Config config;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;
    private final SimpMessageSendingOperations smso;
    private final StatefulRedisModulesConnection<String, String> amRedis;

    public TransactionsStreamConsumer(Config config, StringRedisTemplate transactionsRedis,
            SimpMessageSendingOperations smso, StatefulRedisModulesConnection<String, String> amRedis) {
        this.config = config;
        this.transactionsRedis = transactionsRedis;
        this.smso = smso;
        this.amRedis = amRedis;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setupStreamToWebSocket();
        setupSearchIndex();
    }

    @SuppressWarnings("unchecked")
    private void setupSearchIndex() {
        RediSearchCommands<String, String> commands = amRedis.sync();
        try {
            commands.ftDropindex(SEARCH_INDEX);
        } catch (RedisCommandExecutionException e) {
            if (!e.getMessage().equals("Unknown Index name")) {
                LOGGER.error("Error dropping index: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        CreateOptions<String, String> createOptions = CreateOptions.<String, String>builder().on(DataType.JSON).build();

        commands.ftCreate(SEARCH_INDEX, createOptions,
                Field.text("$.toAccount").as("toAccount").build(),
                Field.text("$.description").as("description").matcher(PhoneticMatcher.ENGLISH).build(),
                Field.text("$.fromAccountName").as("fromAccountName").matcher(PhoneticMatcher.ENGLISH).build(),
                Field.text("$.transactionType").as("transactionType").matcher(PhoneticMatcher.ENGLISH).build());
        LOGGER.info("Created {} index", SEARCH_INDEX);
    }

    private void setupStreamToWebSocket() throws InterruptedException {
        this.container = StreamMessageListenerContainer.create(transactionsRedis.getConnectionFactory(),
                StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofMillis(1000)).build());
        container.start();
        this.subscription = container.receive(StreamOffset.latest(TRANSACTIONS_STREAM), this);
        subscription.await(Duration.ofSeconds(10));
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        // TODO : websocket stuff can be removed, code, pom, etc.
        LOGGER.info("Message received from stream: {}", message);
        String messageString = message.getValue().get("transaction");
        try {
            BankTransaction bt = SerializationUtil.deserializeObject(messageString, BankTransaction.class);
            amRedis.sync().jsonSet("transaction_" + bt.getId(), "$", messageString);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing JSON: {}", e.getMessage());
        }
        smso.convertAndSend(config.getStomp().getTransactionsTopic(), message.getValue());
        LOGGER.info("Websocket message: {}", messageString);
    }

    @Override
    public void destroy() throws Exception {
        if (subscription != null) {
            subscription.cancel();
        }
        if (container != null) {
            container.stop();
        }
    }

}
