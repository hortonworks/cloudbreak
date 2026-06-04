package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.azure.core.management.exception.ManagementException;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

public class AzureRetryExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRetryExecutor.class);

    private static final Duration MAX_INTERVAL = Duration.ofMinutes(1);

    private final AzureExceptionHandler azureExceptionHandler;

    public AzureRetryExecutor(AzureExceptionHandler azureExceptionHandler) {
        this.azureExceptionHandler = azureExceptionHandler;
    }

    public <T> T executeWithConcurrentWriteRetry(Supplier<T> function, Supplier<T> existingResourceSupplier,
            int maxRetries, Duration initialBackoff) {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialBackoff.toMillis());
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(MAX_INTERVAL.toMillis());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.setRetryPolicy(new ConcurrentWriteRetryPolicy(azureExceptionHandler, maxRetries));

        return retryTemplate.execute(context -> {
            try {
                return function.get();
            } catch (ManagementException e) {
                LOGGER.debug("ManagementException caught in retry executor (code: {}). "
                        + "Note: this relies on AzureExceptionHandler re-throwing ManagementException for non-404 errors.",
                        e.getValue() != null ? e.getValue().getCode() : "unknown");
                if (azureExceptionHandler.isConcurrentWrite(e)) {
                    T existingResource = getExistingResourceSafely(existingResourceSupplier);
                    if (existingResource != null) {
                        LOGGER.warn("Concurrent write conflict during create call; resource already exists, continuing.");
                        return existingResource;
                    }
                    int retryNumber = context.getRetryCount() + 1;
                    LOGGER.warn("Concurrent write conflict, retrying ({}/{}).", retryNumber, maxRetries);
                }
                throw e;
            }
        });
    }

    private <T> T getExistingResourceSafely(Supplier<T> existingResourceSupplier) {
        try {
            return azureExceptionHandler.handleException(existingResourceSupplier, null);
        } catch (RuntimeException e) {
            LOGGER.debug("Concurrent write conflict handling: existence check failure details.", e);
            return null;
        }
    }

    private static class ConcurrentWriteRetryPolicy implements RetryPolicy {

        private final SimpleRetryPolicy delegate;

        private final AzureExceptionHandler azureExceptionHandler;

        private ConcurrentWriteRetryPolicy(AzureExceptionHandler azureExceptionHandler, int maxRetries) {
            this.azureExceptionHandler = azureExceptionHandler;
            delegate = new SimpleRetryPolicy(maxRetries + 1, Map.of(ManagementException.class, true), true);
        }

        @Override
        public boolean canRetry(RetryContext context) {
            Throwable lastThrowable = context.getLastThrowable();
            return lastThrowable == null
                    || lastThrowable instanceof ManagementException managementException
                            && azureExceptionHandler.isConcurrentWrite(managementException)
                            && delegate.canRetry(context);
        }

        @Override
        public RetryContext open(RetryContext parent) {
            return delegate.open(parent);
        }

        @Override
        public void close(RetryContext context) {
            delegate.close(context);
        }

        @Override
        public void registerThrowable(RetryContext context, Throwable throwable) {
            delegate.registerThrowable(context, throwable);
        }
    }
}
