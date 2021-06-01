package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.PollingStrategyContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;

public class MaxAttempCancellablePetryStrategy implements PollingStrategy.RetryStrategy {

    private final int defaultMaxAttempts;

    private final CancellationCheck cancellationCheck;

    public MaxAttempCancellablePetryStrategy(int defaultMaxAttempts, CancellationCheck cancellationCheck) {
        this.defaultMaxAttempts = defaultMaxAttempts;
        this.cancellationCheck = cancellationCheck;
    }

    @Override
    public boolean shouldRetry(PollingStrategyContext pollingStrategyContext) {
        if (cancellationCheck != null && cancellationCheck.isCancelled()) {
            throw new CancellationException("Task was cancelled.");
        }
        return pollingStrategyContext.getRetriesAttempted() < defaultMaxAttempts;
    }
}
