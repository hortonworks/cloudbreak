package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;

public class WaitService<T extends WaitObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitService.class);

    public Result<WaitResult, Exception> waitObject(StatusChecker<T> statusChecker, T t, TestContext testContext, Duration interval, int maxAttempts,
            int maxFailure) {
        return waitObject(statusChecker, t, testContext, interval, new TimeoutChecker(maxAttempts), maxFailure);
    }

    public Result<WaitResult, Exception> waitObject(StatusChecker<T> statusChecker, T t, TestContext testContext, Duration interval,
            TimeoutChecker timeoutChecker, int maxFailure) {
        boolean timeout = false;
        int attempts = 0;
        int failures = 0;
        Exception actual = null;
        boolean exit = false;
        long startTime = System.currentTimeMillis();
        while (!timeout && !exit) {
            LOGGER.info("Waiting round {} and elapsed time {} ms", attempts, System.currentTimeMillis() - startTime);
            try {
                statusChecker.refresh(t);
            } catch (Exception ex) {
                LOGGER.debug("Exception occurred during refresh: {}", ex.getMessage(), ex);
                failures++;
                actual = ex;

                if (failures >= maxFailure) {
                    LOGGER.debug("Waiting failure reached the limit which was {}, wait will drop the last exception.", maxFailure);
                    statusChecker.handleException(actual);
                    testContext.setStatuses(statusChecker.getStatuses(t));
                    return Result.exception(actual);
                }
            }
            if (statusChecker.checkStatus(t)) {
                LOGGER.debug(statusChecker.successMessage(t));
                testContext.setStatuses(statusChecker.getStatuses(t));
                return Result.result(WaitResult.SUCCESS);
            }
            sleep(interval, statusChecker.getStatuses(t));
            attempts++;
            timeout = timeoutChecker.checkTimeout();
            LOGGER.info("Checking if wait can exit.");
            exit = statusChecker.exitWaiting(t);
        }
        if (timeout) {
            LOGGER.debug("Wait timeout.");
            statusChecker.handleTimeout(t);
            testContext.setStatuses(statusChecker.getStatuses(t));
            return Result.exception(actual);
        }
        LOGGER.debug("Wait exiting.");
        testContext.setStatuses(statusChecker.getStatuses(t));
        return Result.result(WaitResult.EXIT);
    }

    private void sleep(Duration duration, Map<String, String> statusMap) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            LOGGER.warn("Waiting for '{}' has been interrupted, because of: {}", statusMap, e.getMessage(), e);
        }
    }
}
