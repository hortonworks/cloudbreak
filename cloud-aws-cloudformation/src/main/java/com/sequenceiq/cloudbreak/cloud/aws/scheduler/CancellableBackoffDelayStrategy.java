package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.time.Duration;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.util.RandomUtil;

import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;

public class CancellableBackoffDelayStrategy implements BackoffStrategy {

    private static final int POLLING_INTERVAL = 5;

    private static final int MAX_POLLING_INTERVAL = 30;

    private static final int THOUSAND = 1000;

    private final CancellationCheck cancellationCheck;

    public CancellableBackoffDelayStrategy(CancellationCheck cancellationCheck) {
        this.cancellationCheck = cancellationCheck;
    }

    @Override
    public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
        if (cancellationCheck != null && cancellationCheck.isCancelled()) {
            throw new CancellationException("Task was cancelled.");
        }
        Double secondToWait = Math.min(POLLING_INTERVAL * Math.pow(2, context.retriesAttempted()) + RandomUtil.getInt(POLLING_INTERVAL), MAX_POLLING_INTERVAL);
        return Duration.ofMillis(secondToWait.longValue() * THOUSAND);
    }

    @Override
    public int calculateExponentialDelay(int retriesAttempted, Duration baseDelay, Duration maxBackoffTime) {
        return BackoffStrategy.super.calculateExponentialDelay(retriesAttempted, baseDelay, maxBackoffTime);
    }
}
