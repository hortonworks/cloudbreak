package com.sequenceiq.cloudbreak.service;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service("DefaultRetryService")
public class RetryService implements Retry {

    @Override
    @Retryable(value = ActionWentFailException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionWentFailException {
        return action.get();
    }
}
