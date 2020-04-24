package com.sequenceiq.it.cloudbreak.util.wait.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WaitService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitService.class);

    public Result<WaitResult, Exception> waitWithTimeout(StatusChecker<T> statusChecker, T t, long interval, int maxAttempts, int maxFailure) {
        return waitWithTimeout(statusChecker, t, interval, new TimeoutChecker(maxAttempts), maxFailure);
    }

    public Result<WaitResult, Exception> waitWithTimeout(StatusChecker<T> statusChecker, T t, long interval, TimeoutChecker timeoutChecker,
            int maxFailure) {
        boolean success = false;
        boolean timeout = false;
        int attempts = 0;
        int failures = 0;
        Exception actual = null;
        boolean exit = statusChecker.exitWaiting(t);
        while (!timeout && !exit) {
            LOGGER.debug("Waiting round {}.", attempts);
            try {
                success = statusChecker.checkStatus(t);
            } catch (Exception ex) {
                LOGGER.debug("Exception occurred during waiting: {}", ex.getMessage(), ex);
                failures++;
                actual = ex;
            }
            if (failures >= maxFailure) {
                LOGGER.debug("Waiting failure reached the limit which was {}, wait will drop the last exception.", maxFailure);
                statusChecker.handleException(actual);
                return Result.exception(actual);
            } else if (success) {
                LOGGER.debug(statusChecker.successMessage(t));
                return Result.result(WaitResult.SUCCESS);
            }
            sleep(interval);
            attempts++;
            timeout = timeoutChecker.checkTimeout();
            exit = statusChecker.exitWaiting(t);
        }
        if (timeout) {
            LOGGER.debug("Wait timeout.");
            statusChecker.handleTimeout(t);
            return Result.exception(actual);
        }
        LOGGER.debug("Wait exiting.");
        return Result.result(WaitResult.EXIT);
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred during waiting.", e);
            Thread.currentThread().interrupt();
        }
    }
}
