package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.Retry;

@Service
public class VaultRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRetryService.class);

    private static final String FORBIDDEN_ERROR_MESSAGE = "Status 403 Forbidden";

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Retryable(
            retryFor = Retry.ActionFailedException.class,
            maxAttemptsExpression = "${vault.retry.maxattempt:5}",
            backoff = @Backoff(delayExpression = "${vault.retry.delay:2000}",
                    multiplierExpression = "${vault.retry.multiplier:2}",
                    maxDelayExpression = "${vault.retry.maxdelay:10000}")
    )
    public <T> T tryReadingVault(Supplier<T> action) throws Retry.ActionFailedException {
        return executeVaultOperation(action, "read", MetricType.VAULT_READ_FAILED);
    }

    @Retryable(
            retryFor = Retry.ActionFailedException.class,
            maxAttemptsExpression = "${vault.retry.maxattempt:5}",
            backoff = @Backoff(delayExpression = "${vault.retry.delay:2000}",
                    multiplierExpression = "${vault.retry.multiplier:2}",
                    maxDelayExpression = "${vault.retry.maxdelay:10000}")
    )
    public <T> T tryWritingVault(Supplier<T> action) throws Retry.ActionFailedException {
        return executeVaultOperation(action, "write", MetricType.VAULT_WRITE_FAILED);
    }

    private <T> T executeVaultOperation(Supplier<T> action, String operation, MetricType metricType) {
        try {
            return action.get();
        } catch (CancellationException e) {
            LOGGER.warn("Exception during vault " + operation + ", possible shutdown.");
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error("Exception during vault " + operation, e);
            if (e.getMessage() != null && e.getMessage().contains(FORBIDDEN_ERROR_MESSAGE)) {
                throw e;
            } else {
                metricService.incrementMetricCounter(metricType);
                throw new Retry.ActionFailedException(e.getMessage());
            }
        }
    }
}
