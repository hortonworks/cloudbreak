package com.sequenceiq.cloudbreak.common.tx;

import static com.sequenceiq.cloudbreak.common.tx.HibernateCircuitBreakerConfigProvider.getMaxTransactionTimeThreshold;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

public class HibernateTransactionInterceptor extends EmptyInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateTransactionInterceptor.class);

    private final Map<Transaction, Long> transactionMap = new ConcurrentHashMap<>();

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        long txCompletionTime = System.nanoTime();
        LOGGER.trace("Transaction {} completed at {} nanosec", tx, txCompletionTime);
        Long txStartTime = transactionMap.remove(tx);
        if (txStartTime != null) {
            long transactionTime = NANOSECONDS.toMillis(txCompletionTime - txStartTime);
            recordTransactionMetrics(transactionTime);
            if (transactionTime > getMaxTransactionTimeThreshold()) {
                LOGGER.warn("Transaction time warning (>{}ms): {}ms", getMaxTransactionTimeThreshold(), transactionTime);
            }
        }
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        long txStartTime = System.nanoTime();
        transactionMap.put(tx, txStartTime);
        LOGGER.trace("Transaction {} begin at {} nanosec", tx, txStartTime);
    }

    private void recordTransactionMetrics(long transactionTimeMs) {
        MetricService metricService = HibernateMetricsProvider.getMetricService();
        if (metricService == null) {
            return;
        }
        boolean warning = transactionTimeMs > getMaxTransactionTimeThreshold();
        String outcome = warning ? "warning" : "ok";
        metricService.recordTimerMetric(HibernateMetricType.TRANSACTION_DURATION,
                Duration.ofMillis(transactionTimeMs), "outcome", outcome);
        if (warning) {
            metricService.incrementMetricCounter(HibernateMetricType.TRANSACTION_WARNING);
        }
    }
}