package com.sequenceiq.cloudbreak.polling;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PollingService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);

    private static final int DEFAULT_MAX_CONSECUTIVE_FAILURES = 5;

    /**
     * Executes a {@link StatusCheckerTask} until it signals success, or the
     * maximum attempts are reached. A {@link StatusCheckerTask} has no
     * restrictions about what kind of tasks it should do, it just needs to
     * return if the task succeeded or not. If maxAttempts is lower than 0,
     * there will be no timeout.
     *
     * @param interval    sleeps this many milliseconds between status checking attempts
     * @param maxAttempts signals how many times will the status check be executed before timeout
     */
    public Pair<PollingResult, Exception> pollWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval,
            int maxAttempts, int maxConsecutiveFailures) {
        return pollWithTimeout(statusCheckerTask, t, interval, new AttemptBasedTimeoutChecker(maxAttempts), maxConsecutiveFailures);
    }

    public Pair<PollingResult, Exception> pollWithAbsoluteTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval,
            long maximumWaitTimeInSeconds, int maxConsecutiveFailures) {
        return pollWithTimeout(statusCheckerTask, t, interval, new AbsolutTimeBasedTimeoutChecker(maximumWaitTimeInSeconds), maxConsecutiveFailures);
    }

    public Pair<PollingResult, Exception> pollWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval, TimeoutChecker timeoutChecker,
            int maxConsecutiveFailures) {
        boolean success = false;
        boolean timeout = false;
        int attempts = 0;
        int consecutiveFailures = 0;
        Exception actual = null;
        boolean exit = false;
        if (statusCheckerTask.initialExitCheck(t)) {
            exit = statusCheckerTask.exitPolling(t);
        }
        while (!timeout && !exit) {
            LOGGER.debug("Polling attempt {}.", attempts);
            try {
                success = statusCheckerTask.checkStatus(t);
                consecutiveFailures = 0;
            } catch (Exception ex) {
                consecutiveFailures++;
                actual = ex;
                LOGGER.debug("Exception occurred in the polling: {}. Number of consecutive failures: [{}/{}]",
                        ex.getMessage(), consecutiveFailures, maxConsecutiveFailures, ex);
            }
            if (consecutiveFailures >= maxConsecutiveFailures) {
                LOGGER.debug("Polling failure reached the limit which was {}, poller will drop the last exception.", maxConsecutiveFailures);
                statusCheckerTask.handleException(actual);
                return new ImmutablePair<>(PollingResult.FAILURE, actual);
            } else if (success) {
                LOGGER.debug(statusCheckerTask.successMessage(t));
                LOGGER.debug("Set the number of consecutive failures to 0, since we received a positive answer. Original number of consecutiveFailures: {}",
                        consecutiveFailures);
                return new ImmutablePair<>(PollingResult.SUCCESS, actual);
            }
            sleep(interval);
            attempts++;
            timeout = timeoutChecker.checkTimeout();
            exit = statusCheckerTask.exitPolling(t);
        }
        if (timeout) {
            LOGGER.debug("Poller timeout.");
            statusCheckerTask.handleTimeout(t);
            return new ImmutablePair<>(PollingResult.TIMEOUT, actual);
        }
        LOGGER.debug("Poller exiting.");
        return new ImmutablePair<>(PollingResult.EXIT, actual);
    }

    public PollingResult pollWithAbsoluteTimeout(StatusCheckerTask<T> statusCheckerTask, T t, int interval, long maximumWaitTimeInSeconds) {
        return pollWithAbsoluteTimeout(statusCheckerTask, t, interval, maximumWaitTimeInSeconds, DEFAULT_MAX_CONSECUTIVE_FAILURES).getLeft();
    }

    public PollingResult pollWithAttempt(StatusCheckerTask<T> statusCheckerTask, T t, int interval, int maxAttempts) {
        return pollWithTimeout(statusCheckerTask, t, interval, maxAttempts, DEFAULT_MAX_CONSECUTIVE_FAILURES).getLeft();
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred during polling.", e);
            Thread.currentThread().interrupt();
        }
    }
}
