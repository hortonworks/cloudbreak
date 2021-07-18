package com.sequenceiq.environment.experience;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RetryableWebTarget {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryableWebTarget.class);

    private static final int BACKOFF_DELAY = 500;

    private static final int MAX_ATTEMPTS = 5;

    @Retryable(maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = BACKOFF_DELAY))
    public Response get(Invocation.Builder call) {
        LOGGER.info("Retryable GET called [{}] with the maximum amount of retry of {} and with the backoff delay of {}", call.toString(), MAX_ATTEMPTS,
                BACKOFF_DELAY);
        return call.get();
    }

    @Retryable(maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = BACKOFF_DELAY))
    public Response delete(Invocation.Builder call) {
        LOGGER.info("Retryable DELETE called [{}] with the maximum amount of retry of {} and with the backoff delay of {}", call.toString(), MAX_ATTEMPTS,
                BACKOFF_DELAY);
        return call.delete();
    }

}
