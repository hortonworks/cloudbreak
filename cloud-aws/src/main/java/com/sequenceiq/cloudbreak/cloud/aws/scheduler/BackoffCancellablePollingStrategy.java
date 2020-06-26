package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import com.amazonaws.waiters.PollingStrategy;

public class BackoffCancellablePollingStrategy {

    private static final int DEFAULT_MAX_ATTEMPTS = 1000;

    private BackoffCancellablePollingStrategy() {
    }

    public static PollingStrategy getBackoffCancellablePollingStrategy(CancellationCheck cancellationCheck) {
        return new PollingStrategy(new MaxAttempCancellablePetryStrategy(DEFAULT_MAX_ATTEMPTS, cancellationCheck), new BackoffDelayStrategy());
    }
}
