package com.sequenceiq.cloudbreak.ccmimpl.util;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.Immutable;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.StopStrategy;

/**
 * Factory class for instances of {@link StopStrategy}.
 */
public final class StopStrategyFactory {

    private static final StopStrategy STOP_IMMEDIATELY = new StopImmediately();

    private StopStrategyFactory() {
    }

    /**
     * Returns a stop strategy that will stop retrying when the specified date and time is reached. Will always
     * execute the callable at least one time.
     *
     * @param stopDateTime the date and time to stop retrying
     * @return a stop strategy that stops at a specified date and time
     */
    public static StopStrategy waitUntilDateTime(ZonedDateTime stopDateTime) {
        long millisecondsUntilStop = ChronoUnit.MILLIS.between(ZonedDateTime.now(), stopDateTime);

        if (millisecondsUntilStop < 0) {
            return STOP_IMMEDIATELY;
        }

        return StopStrategies.stopAfterDelay(millisecondsUntilStop, TimeUnit.MILLISECONDS);
    }

    @Immutable
    private static final class StopImmediately implements StopStrategy {

        StopImmediately() {
        }

        @Override
        public boolean shouldStop(Attempt failedAttempt) {
            return true;
        }
    }
}
