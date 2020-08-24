package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.PollingStrategyContext;

public class BackoffDelayStrategy implements PollingStrategy.DelayStrategy {
    private static final int POLLING_INTERVAL = 5;

    private static final int MAX_POLLING_INTERVAL = 30;

    private static final Random RANDOM = ThreadLocalRandom.current();

    private static final int THOUSAND = 1000;

    @Override
    public void delayBeforeNextRetry(PollingStrategyContext pollingStrategyContext) throws InterruptedException {
        int requestAttempted = pollingStrategyContext.getRetriesAttempted();
        Double secondToWait = Math.min(POLLING_INTERVAL * Math.pow(2, requestAttempted) + RANDOM.nextInt(POLLING_INTERVAL), MAX_POLLING_INTERVAL);
        Thread.sleep(secondToWait.longValue() * THOUSAND);
    }
}
