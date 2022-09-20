package com.redis.demos.redisbank.transactions;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redis.demos.redisbank.Account;
import com.redis.demos.redisbank.SerializationUtil;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;

@Component
public class TransactionsStreamConsumer
        implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsStreamConsumer.class);
    private static final String TRANSACTIONS_STREAM = "transactions";

    private final StringRedisTemplate transactionsRedis;
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
    private final StatefulRedisModulesConnection<String, String> srmc;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;

    public TransactionsStreamConsumer(StringRedisTemplate transactionsRedis,
            StatefulRedisModulesConnection<String, String> srmc) {
        this.transactionsRedis = transactionsRedis;
        this.srmc = srmc;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.container = StreamMessageListenerContainer.create(transactionsRedis.getConnectionFactory(),
                StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofMillis(1000)).build());
        container.start();
        this.subscription = container.receive(StreamOffset.fromStart(TRANSACTIONS_STREAM), this);
        subscription.await(Duration.ofSeconds(10));
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        String messageString = message.getValue().get("transaction");
        LOGGER.info("Message received from stream: {}", messageString);

        try {
            BankTransaction bankTransaction = SerializationUtil.deserializeObject(messageString, BankTransaction.class);
            Number balanceAfter = nf.parse(bankTransaction.getBalanceAfter());
            String accountString;
            Account account;

            RedisModulesCommands<String, String> commands = srmc.sync();

            try {
                accountString = commands.jsonGet(bankTransaction.getToAccount());
                if (StringUtils.hasText(accountString)) {
                    account = SerializationUtil.deserializeObject(accountString, Account.class);
                    account.setBalance(balanceAfter.doubleValue());
                    commands.jsonSet(bankTransaction.getToAccount(), "$", SerializationUtil.serializeObject(account));
                } else {
                    account = new Account();
                    account.setAccountName(bankTransaction.getToAccountName());
                    account.setIban(bankTransaction.getToAccount());
                    account.setBalance(balanceAfter.doubleValue());
                    commands.jsonSet(bankTransaction.getToAccount(), "$", SerializationUtil.serializeObject(account));
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred: {}", e.getMessage());
                account = new Account();
                account.setAccountName(bankTransaction.getToAccountName());
                account.setIban(bankTransaction.getToAccount());
                account.setBalance(balanceAfter.doubleValue());
                commands.jsonSet(bankTransaction.getToAccount(), "$", SerializationUtil.serializeObject(account));
            }

        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing JSON: {}", e.getMessage());
        } catch (ParseException e) {
            LOGGER.error("Error parsing number: {}", e.getMessage());
        }
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
