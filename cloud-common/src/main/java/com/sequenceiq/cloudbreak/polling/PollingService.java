package com.sequenceiq.cloudbreak.polling;

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
    public ExtendedPollingResult pollWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval,
            int maxAttempts, int maxConsecutiveFailures) {
        return pollWithTimeout(statusCheckerTask, t, interval, new AttemptBasedTimeoutChecker(maxAttempts), maxConsecutiveFailures);
    }

    public ExtendedPollingResult pollWithAbsoluteTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval,
            long maximumWaitTimeInSeconds, int maxConsecutiveFailures) {
        return pollWithTimeout(statusCheckerTask, t, interval, new AbsolutTimeBasedTimeoutChecker(maximumWaitTimeInSeconds), maxConsecutiveFailures);
    }

    public ExtendedPollingResult pollWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval, TimeoutChecker timeoutChecker,
            int maxConsecutiveFailures) {
        boolean success = false;
        boolean timeout = false;
        int attempts = 0;
        int consecutiveFailures = 0;
        long defaultInterval = interval;
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
                interval = defaultInterval;
            } catch (Exception ex) {
                consecutiveFailures++;
                actual = ex;
                LOGGER.debug("Exception occurred in the polling: {}. Number of consecutive failures: [{}/{}]",
                        ex.getMessage(), consecutiveFailures, maxConsecutiveFailures, ex);
                interval = statusCheckerTask.increasePollingBackoff(defaultInterval, consecutiveFailures);
            }
            if (consecutiveFailures >= maxConsecutiveFailures) {
                LOGGER.debug("Polling failure reached the limit which was {}, poller will drop the last exception.", maxConsecutiveFailures);
                statusCheckerTask.sendFailureEvent(t);
                statusCheckerTask.handleException(actual);
                return new ExtendedPollingResult.ExtendedPollingResultBuilder()
                        .failure()
                        .withException(actual)
                        .build();
            } else if (success) {
                LOGGER.debug(statusCheckerTask.successMessage(t));
                LOGGER.debug("Set the number of consecutive failures to 0, since we received a positive answer. Original number of consecutiveFailures: {}",
                        consecutiveFailures);
                return new ExtendedPollingResult.ExtendedPollingResultBuilder()
                        .success()
                        .build();
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception occurred during polling.", e);
                Thread.currentThread().interrupt();
                statusCheckerTask.sendFailureEvent(t);
                statusCheckerTask.handleException(e);
                return new ExtendedPollingResult.ExtendedPollingResultBuilder()
                        .failure()
                        .withException(e)
                        .build();
            }
            attempts++;
            timeout = timeoutChecker.checkTimeout();
            exit = statusCheckerTask.exitPolling(t);
            statusCheckerTask.sendWarningTimeoutEventIfNecessary(t);
        }
        if (timeout) {
            LOGGER.debug("Poller timeout.");
            statusCheckerTask.sendTimeoutEvent(t);
            statusCheckerTask.handleTimeout(t);
            return new ExtendedPollingResult.ExtendedPollingResultBuilder()
                    .timeout()
                    .withException(actual)
                    .withPayload(statusCheckerTask.getFailedInstancePrivateIds())
                    .build();
        }
        LOGGER.debug("Poller exiting.");
        return new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .exit()
                .withException(actual)
                .build();
    }

    public ExtendedPollingResult pollWithAbsoluteTimeout(StatusCheckerTask<T> statusCheckerTask, T t, int interval, long maximumWaitTimeInSeconds) {
        return pollWithAbsoluteTimeout(statusCheckerTask, t, interval, maximumWaitTimeInSeconds, DEFAULT_MAX_CONSECUTIVE_FAILURES);
    }

    public ExtendedPollingResult pollWithAttempt(StatusCheckerTask<T> statusCheckerTask, T t, int interval, int maxAttempts) {
        return pollWithTimeout(statusCheckerTask, t, interval, maxAttempts, DEFAULT_MAX_CONSECUTIVE_FAILURES);
    }

}
