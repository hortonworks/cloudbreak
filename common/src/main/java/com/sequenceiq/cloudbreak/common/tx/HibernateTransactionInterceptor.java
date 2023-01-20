package com.sequenceiq.cloudbreak.common.tx;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;

public class HibernateTransactionInterceptor extends EmptyInterceptor {

    private static final long DEFAULT_TRANSACTION_MAX_TIME_WARNING = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateTransactionInterceptor.class);

    private final Map<Transaction, Long> transactionMap = new ConcurrentHashMap<>();

    private final long maxTransactionTimeThreshold;

    public HibernateTransactionInterceptor() {
        this.maxTransactionTimeThreshold = StaticApplicationContext.getEnvironmentProperty("hibernate.transaction.warning.max.time",
                Long.class, DEFAULT_TRANSACTION_MAX_TIME_WARNING);
    }

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        long txCompletionTime = System.nanoTime();
        LOGGER.trace("Transaction {} completed at {} nanosec", tx, txCompletionTime);
        Long txStartTime = transactionMap.remove(tx);
        if (txStartTime != null) {
            long transactionTime = NANOSECONDS.toMillis(txCompletionTime - txStartTime);
            if (transactionTime > maxTransactionTimeThreshold) {
                LOGGER.warn("Transaction time warning (>{}ms): {}ms", maxTransactionTimeThreshold, transactionTime);
            }
        }
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        long txStartTime = System.nanoTime();
        transactionMap.put(tx, txStartTime);
        LOGGER.trace("Transaction {} begin at {} nanosec", tx, txStartTime);
    }

}