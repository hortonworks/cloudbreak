package com.sequenceiq.cloudbreak.common.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Service
public class TransactionMetricsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMetricsService.class);

    private static final long TX_DURATION_OK = 1000L;

    private static final long TX_DURATION_ERROR = 3000L;

    @Autowired
    private Clock clock;

    @Value("${cb.log.transaction.stacktrace:true}")
    private boolean logTransactionStacktrace;

    @Inject
    private MetricService metricService;

    public TransactionMetricsContext createTransactionMetricsContext() {
        long start = clock.getCurrentTimeMillis();
        TransactionMetricsContext transactionMetricsContext = new TransactionMetricsContext(start);
        LOGGER.debug("Transaction context has been created, transactionMetricsId: {}", transactionMetricsContext.getTxId());
        return transactionMetricsContext;
    }

    public void processTransactionDuration(TransactionMetricsContext transactionMetricsContext) {
        long duration = clock.getCurrentTimeMillis() - transactionMetricsContext.getStart();
        metricService.recordTransactionTime(transactionMetricsContext, duration);
        if (TX_DURATION_ERROR < duration) {
            if (logTransactionStacktrace) {
                LOGGER.error("Transaction duration was critical, transactionMetricsId: {}, took {}ms at: {}", transactionMetricsContext.getTxId(),
                        duration, generateStackTrace());
            } else {
                LOGGER.error("Transaction duration was critical, transactionMetricsId: {}, took {}ms", transactionMetricsContext.getTxId(), duration);
            }
        } else if (TX_DURATION_OK < duration) {
            if (logTransactionStacktrace) {
                LOGGER.info("Transaction duration was high, transactionMetricsId: {}, took {}ms at: {}", transactionMetricsContext.getTxId(),
                        duration, generateStackTrace());
            } else {
                LOGGER.info("Transaction duration was high, transactionMetricsId: {}, took {}ms", transactionMetricsContext.getTxId(), duration);
            }
        } else {
            LOGGER.debug("Transaction duration was ok, transactionMetricsId: {}, took {}ms", transactionMetricsContext.getTxId(), duration);
        }
    }

    private String generateStackTrace() {
        return String.join("\n\t", Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

}
