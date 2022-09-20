package com.redis.demos.redisbankpfm.transactions;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redis.demos.redisbankpfm.SerializationUtil;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.timeseries.Sample;

@Component
public class TransactionsStreamConsumer
        implements InitializingBean, DisposableBean, StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsStreamConsumer.class);
    private static final String TRANSACTIONS_STREAM = "transactions";

    private final StringRedisTemplate transactionsRedis;
    private final StatefulRedisModulesConnection<String, String> pfmRedis;
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;

    public TransactionsStreamConsumer(StringRedisTemplate transactionsRedis,
            StatefulRedisModulesConnection<String, String> pfmRedis) {
        this.transactionsRedis = transactionsRedis;
        this.pfmRedis = pfmRedis;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        subscribeToTransactionsStream();
    }

    private void subscribeToTransactionsStream() throws InterruptedException {
        this.container = StreamMessageListenerContainer.create(transactionsRedis.getConnectionFactory(),
                StreamMessageListenerContainerOptions.builder().pollTimeout(Duration.ofMillis(1000)).build());
        container.start();
        this.subscription = container.receive(StreamOffset.latest(TRANSACTIONS_STREAM), this);
        subscription.await(Duration.ofSeconds(10));
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        LOGGER.info("Message received from stream: {}", message);
        String messageString = message.getValue().get("transaction");

        try {
            BankTransaction bankTransaction = SerializationUtil.deserializeObject(messageString, BankTransaction.class);

            updateBiggestSpenders(bankTransaction);
            updateBalanceOverTime(bankTransaction);

        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing JSON: {}", e.getMessage());
        }
    }

    private void updateBalanceOverTime(BankTransaction bankTransaction) {
        String timeSeriesKey = "balance_over_time_" + bankTransaction.getToAccount();

        try {
            Number balanceAfter = nf.parse(bankTransaction.getBalanceAfter());
            Sample sample = Sample.of(balanceAfter.doubleValue());
            pfmRedis.sync().tsAdd(timeSeriesKey, sample);
        } catch (ParseException e) {
            LOGGER.error("Error parsing transaction amount: {}", e.getMessage());
        }

    }

    private void updateBiggestSpenders(BankTransaction bankTransaction) {
        String fromAccount = bankTransaction.getFromAccount();
        String sortedSetKey = "biggest_spenders_" + bankTransaction.getToAccount();
        try {
            Number amount = nf.parse(bankTransaction.getAmount());
            pfmRedis.sync().zadd(sortedSetKey, amount.doubleValue(), fromAccount);
        } catch (ParseException e) {
            LOGGER.error("Error parsing transaction amount: {}", e.getMessage());
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
