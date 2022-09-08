package com.redis.demos.redisbank.transactions;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.util.Optional;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redis.demos.redisbank.Account;
import com.redis.demos.redisbank.AccountRepository;
import com.redis.demos.redisbank.SerializationUtil;

@Component
public class TransactionsStreamConsumer
        implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsStreamConsumer.class);
    private static final String TRANSACTIONS_STREAM = "transactions";

    private final StringRedisTemplate redis;
    private final NumberFormat nf = NumberFormat.getCurrencyInstance();

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;
    private final AccountRepository ar;

    public TransactionsStreamConsumer(StringRedisTemplate redis, AccountRepository ar) {
        this.redis = redis;
        this.ar = ar;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.container = StreamMessageListenerContainer.create(redis.getConnectionFactory(),
                StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofMillis(1000)).build());
        container.start();
        this.subscription = container.receive(StreamOffset.fromStart(TRANSACTIONS_STREAM), this);
        subscription.await(Duration.ofSeconds(10));
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        LOGGER.info("Message received from stream: {}", message);
        String messageString = message.getValue().get("transaction");

        try {
            BankTransaction bankTransaction = SerializationUtil.deserializeObject(messageString, BankTransaction.class);
            Optional<Account> accountOptional = ar.findByIban(bankTransaction.getToAccount());
            Number balanceAfter = nf.parse(bankTransaction.getBalanceAfter());
            if (accountOptional.isPresent()) {
                Account account = accountOptional.get();
                account.setBalance(balanceAfter.doubleValue());
                ar.save(account);

            } else {
                Account newAccount = new Account();
                newAccount.setAccountName(bankTransaction.getToAccountName());
                newAccount.setIban(bankTransaction.getToAccount());
                newAccount.setBalance(balanceAfter.doubleValue());
                ar.save(newAccount);
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
