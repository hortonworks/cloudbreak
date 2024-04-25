package com.sequenceiq.freeipa.client;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.CheckedFunction;
import com.sequenceiq.cloudbreak.util.CheckedRunnable;
import com.sequenceiq.cloudbreak.util.CheckedSupplier;

@Service
public class RetryableFreeIpaClientService {

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public <T> T invokeWithRetries(CheckedFunction<Void, T, Exception> f) throws Exception {
        return f.apply(null);
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public <T> T invokeWithRetries(CheckedSupplier<T, Exception> f) throws Exception {
        return f.get();
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void invokeWithRetries(CheckedRunnable f) throws Exception {
        f.run();
    }
}
