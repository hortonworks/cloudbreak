package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class PollingService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);

    /**
     * Executes a {@link StatusCheckerTask} until it signals success, or the
     * maximum attempts are reached. A {@link StatusCheckerTask} has no
     * restrictions about what kind of tasks it should do, it just needs to
     * return if the task succeeded or not. If maxAttempts is lower than 0,
     * there will be no timeout.
     * 
     * @param interval sleeps this many milliseconds between status checking attempts
     * @param maxAttempts signals how many times will the status check be executed before timeout
     */
    public void pollWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, int interval, int maxAttempts) {
        boolean success = false;
        boolean timeout = false;
        int attempts = 0;
        MDCBuilder.buildMdcContext();
        while (!success && !timeout && !statusCheckerTask.exitPoller(t)) {
            LOGGER.info("Polling attempt {}.", attempts);
            success = statusCheckerTask.checkStatus(t);
            if (success) {
                LOGGER.info(statusCheckerTask.successMessage(t));
                return;
            }
            sleep(interval);
            attempts++;
            if (maxAttempts > 0) {
                timeout = attempts >= maxAttempts;
            }
        }
        if (timeout) {
            statusCheckerTask.handleTimeout(t);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted exception occurred during polling.", e);
            Thread.currentThread().interrupt();
        }
    }

}