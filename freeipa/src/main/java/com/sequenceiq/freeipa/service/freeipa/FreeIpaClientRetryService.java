package com.sequenceiq.freeipa.service.freeipa;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClientCallable;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientRunnable;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;

@Service
public class FreeIpaClientRetryService {

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void retryWhenRetryableWithoutValue(FreeIpaClientRunnable runnable) throws FreeIpaClientException {
        runnable.run();
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public <T> T retryWhenRetryableWithValue(FreeIpaClientCallable<T> callable) throws FreeIpaClientException {
        return callable.run();
    }
}
