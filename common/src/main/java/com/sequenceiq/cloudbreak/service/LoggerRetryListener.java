package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class LoggerRetryListener extends RetryListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerRetryListener.class);

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        LOGGER.debug("Retry threw {} exception {}: {}", context.getRetryCount(), throwable.getClass().getSimpleName(), throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        int retryCount = context.getRetryCount();
        if (retryCount > 0) {
            String closeReason = calculateCloseReason(context, throwable);
            LOGGER.debug("After {} attempt, retry {}", retryCount, closeReason);
        }
    }

    private String calculateCloseReason(RetryContext context, Throwable throwable) {
        if (isExhausted(context)) {
            if (throwable != null) {
                return String.format("exhausted, last exception was: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage());
            } else {
                return "exhausted";
            }
        } else {
            return "finished successfully";
        }
    }

    private boolean isExhausted(RetryContext context) {
        return context.hasAttribute(RetryContext.EXHAUSTED);
    }
}
