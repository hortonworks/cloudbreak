package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import com.amazonaws.waiters.PollingStrategy;

public class SlowStartCancellablePollingStrategy {

    private static final int DEFAULT_MAX_ATTEMPTS = 1000;

    private SlowStartCancellablePollingStrategy() {
    }

    public static PollingStrategy getExpectedRuntimeCancellablePollingStrategy(CancellationCheck cancellationCheck, int expectedRuntimeSeconds) {
        return new PollingStrategy(new MaxAttempCancellablePetryStrategy(DEFAULT_MAX_ATTEMPTS, cancellationCheck),
                new SlowStartDelayStrategy(expectedRuntimeSeconds));
    }
}