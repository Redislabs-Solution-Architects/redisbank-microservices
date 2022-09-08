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
import com.redis.lettucemod.search.Field;

import io.lettuce.core.RedisCommandExecutionException;

@Component
public class TransactionsStreamConsumer
        implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {

    private static final String SEARCH_INDEX = "transactions_idx";

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsStreamConsumer.class);
    private static final String TRANSACTIONS_STREAM = "transactions";

    private final StringRedisTemplate redis;
    private final Config config;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;
    private final SimpMessageSendingOperations smso;
    private final BankTransactionRepository btr;
    private final StatefulRedisModulesConnection<String, String> srmc;

    public TransactionsStreamConsumer(Config config, StringRedisTemplate redis, SimpMessageSendingOperations smso,
            BankTransactionRepository btr, StatefulRedisModulesConnection<String, String> srmc) {
        this.config = config;
        this.redis = redis;
        this.smso = smso;
        this.btr = btr;
        this.srmc = srmc;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setupStreamToWebSocket();
        setupSearchIndex();
    }

    private void setupSearchIndex() {
        RediSearchCommands<String, String> commands = srmc.sync();
        try {
            commands.dropindex(SEARCH_INDEX);
        } catch (RedisCommandExecutionException e) {
            if (!e.getMessage().equals("Unknown Index name")) {
                LOGGER.error("Error dropping index: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
        commands.create(SEARCH_INDEX,
                Field.text("toAccount").build(),
                Field.text("description").matcher(Field.TextField.PhoneticMatcher.ENGLISH).build(),
                Field.text("fromAccountName").matcher(Field.TextField.PhoneticMatcher.ENGLISH).build(),
                Field.text("transactionType").matcher(Field.TextField.PhoneticMatcher.ENGLISH).build());
        LOGGER.info("Created {} index", SEARCH_INDEX);
    }

    private void setupStreamToWebSocket() throws InterruptedException {
        this.container = StreamMessageListenerContainer.create(redis.getConnectionFactory(),
                StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofMillis(1000)).build());
        container.start();
        this.subscription = container.receive(StreamOffset.fromStart(TRANSACTIONS_STREAM), this);
        subscription.await(Duration.ofSeconds(10));
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        //TODO : websocket stuff can be removed, code, pom, etc.
        LOGGER.info("Message received from stream: {}", message);
        String messageString = message.getValue().get("transaction");
        try {
            btr.save(SerializationUtil.deserializeObject(messageString, BankTransaction.class));
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
