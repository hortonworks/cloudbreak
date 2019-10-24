package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.Retry;

@Service
public class VaultRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRetryService.class);

    private final MetricService metricService;

    public VaultRetryService(MetricService metricService) {
        this.metricService = metricService;
    }

    @Retryable(
            value = Retry.ActionFailedException.class,
            maxAttemptsExpression = "${vault.retry.maxattempt:5}",
            backoff = @Backoff(delayExpression = "${vault.retry.delay:2000}",
                    multiplierExpression = "${vault.retry.multiplier:2}",
                    maxDelayExpression = "${vault.retry.maxdelay:10000}")
    )
    public <T> T tryReadingVault(Supplier<T> action) throws Retry.ActionFailedException {
        try {
            return action.get();
        } catch (RuntimeException e) {
            LOGGER.error("Exception during reading vault", e);
            metricService.incrementMetricCounter(MetricType.VAULT_READ_FAILED);
            throw new Retry.ActionFailedException(e.getMessage());
        }
    }

    @Retryable(
            value = Retry.ActionFailedException.class,
            maxAttemptsExpression = "${vault.retry.maxattempt:5}",
            backoff = @Backoff(delayExpression = "${vault.retry.delay:2000}",
                    multiplierExpression = "${vault.retry.multiplier:2}",
                    maxDelayExpression = "${vault.retry.maxdelay:10000}")
    )
    public <T> T tryWritingVault(Supplier<T> action) throws Retry.ActionFailedException {
        try {
            return action.get();
        } catch (RuntimeException e) {
            LOGGER.error("Exception during writing vault", e);
            metricService.incrementMetricCounter(MetricType.VAULT_READ_FAILED);
            throw new Retry.ActionFailedException(e.getMessage());
        }
    }
}
