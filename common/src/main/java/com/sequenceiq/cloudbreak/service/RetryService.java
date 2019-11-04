package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service("DefaultRetryService")
public class RetryService implements Retry {

    @Override
    @Retryable(value = ActionFailedException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionFailedException {
        return action.get();
    }

    @Override
    @Retryable(
            value = ActionFailedException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    public <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException {
        return action.get();
    }
}
