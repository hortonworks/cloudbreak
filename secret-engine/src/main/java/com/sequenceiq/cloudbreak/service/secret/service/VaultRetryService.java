package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.retry.Retry;
import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine;

@Service
@ConditionalOnBean({VaultKvV2Engine.class, VaultConfig.class})
public class VaultRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRetryService.class);

    private static final String FORBIDDEN_ERROR_MESSAGE = "Status 403 Forbidden";

    private static final String INVALID_TOKEN_ERROR_MESSAGE = "invalid token";

    private static final String CONNECTION_POOL_SHUT_DOWN = "Connection pool shut down";

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @Inject
    private SessionManager sessionManager;

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
            String message = e.getMessage();
            if (message != null && message.contains(FORBIDDEN_ERROR_MESSAGE) && message.contains(INVALID_TOKEN_ERROR_MESSAGE)) {
                LOGGER.info("Vault session token is expired or invalid during {}, revoking it and forcing a re-login before retrying", operation);
                forceVaultReLogin();
                throw new VaultRetryException(message, e, operation, metricType);
            }
            LOGGER.warn("Exception during vault " + operation, e);
            if (message != null && (message.contains(FORBIDDEN_ERROR_MESSAGE) || message.equals(CONNECTION_POOL_SHUT_DOWN))) {
                throw e;
            } else {
                throw new VaultRetryException(message, e, operation, metricType);
            }
        }
    }

    private void forceVaultReLogin() {
        try {
            if (sessionManager instanceof LifecycleAwareSessionManager lifecycleAwareSessionManager) {
                lifecycleAwareSessionManager.revoke();
                LOGGER.info("Revoked the invalid Vault session token, the next attempt will re-login and obtain a fresh token");
            } else {
                LOGGER.warn("Cannot force Vault re-authentication, session manager is not a LifecycleAwareSessionManager: {}",
                        sessionManager.getClass().getName());
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to revoke the stale Vault session token, proceeding with retry anyway", ex);
        }
    }

    @Recover
    public <T> T recover(VaultRetryException exception) {
        LOGGER.error("Retry exhausted during vault {}", exception.getOperation(), exception);
        metricService.incrementMetricCounter(exception.getMetricType());
        throw exception;
    }

    @Recover
    public <T> T recover(RuntimeException exception) {
        throw exception;
    }

    static class VaultRetryException extends Retry.ActionFailedException {

        private final String operation;

        private final MetricType metricType;

        VaultRetryException(String message, Exception cause, String operation, MetricType metricType) {
            super(message, cause);
            this.operation = operation;
            this.metricType = metricType;
        }

        public String getOperation() {
            return operation;
        }

        public MetricType getMetricType() {
            return metricType;
        }
    }
}
