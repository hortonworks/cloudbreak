package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.PollingStrategyContext;

/**
 * A polling strategy which is less aggressive early, and gets more aggressive once the
 * provided expectedRuntime is reached.
 */
public class SlowStartDelayStrategy implements PollingStrategy.DelayStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlowStartDelayStrategy.class);

    private static final Random RANDOM = ThreadLocalRandom.current();

    private static final int MAX_POLLING_INTERVAL_SECONDS = 60;

    private static final int STABLE_POLLING_INTERVAL_SECONDS_MAX = 5;

    private static final int STABLE_POLLING_INTERVAL_SECONDS_MIN = 1;

    private static final double FIRST_POLL_RATIO = 0.75;

    private static final double SECOND_POLL_RATIO = 0.2;

    private static final double THOUSAND = 1000.0;

    private final int expectedRuntimeSeconds;

    private final long msToWaitRequest0;
    private final long msToWaitRequest1;

    public SlowStartDelayStrategy(int expectedRuntimeSeconds) {
        this.expectedRuntimeSeconds = expectedRuntimeSeconds;
        msToWaitRequest0 = (long) (THOUSAND * Math.min(
                Math.max(FIRST_POLL_RATIO * expectedRuntimeSeconds, STABLE_POLLING_INTERVAL_SECONDS_MIN),
                MAX_POLLING_INTERVAL_SECONDS));
        msToWaitRequest1 = (long) (THOUSAND * Math.min(
                Math.max(SECOND_POLL_RATIO * expectedRuntimeSeconds, STABLE_POLLING_INTERVAL_SECONDS_MIN),
                MAX_POLLING_INTERVAL_SECONDS));
    }

    @Override
    public void delayBeforeNextRetry(PollingStrategyContext pollingStrategyContext) throws InterruptedException {
        int requestNumber = pollingStrategyContext.getRetriesAttempted();

        // Start polling at 75% of minExpectedRuntime.
        // Next at 20% of minExpectedRuntime
        // After this random between min and max

        long msToWait;
        if (requestNumber == 0) {
            msToWait = msToWaitRequest0;
        } else if (requestNumber == 1) {
            msToWait = msToWaitRequest1;
        } else {
            msToWait = (long) (THOUSAND * Math.max(STABLE_POLLING_INTERVAL_SECONDS_MIN,
                    RANDOM.nextInt(STABLE_POLLING_INTERVAL_SECONDS_MAX)));
        }
        LOGGER.info("Polling attempt: {}, Sleeping for: {}ms for request: {}. ExpectedRuntime={}s",
                requestNumber, msToWait, pollingStrategyContext.getOriginalRequest(), expectedRuntimeSeconds);
        Thread.sleep(msToWait);
    }
}