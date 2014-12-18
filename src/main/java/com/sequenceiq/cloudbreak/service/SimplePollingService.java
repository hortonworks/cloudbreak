package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class SimplePollingService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePollingService.class);

    /**
     * Executes a {@link com.sequenceiq.cloudbreak.service.StatusCheckerTask} until it signals success, or the
     * maximum attempts are reached. A {@link com.sequenceiq.cloudbreak.service.StatusCheckerTask} has no
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
        boolean exit = exitPolling(t);
        while (!success && !timeout && !exit) {
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
            exit = exitPolling(t);
        }
        if (timeout) {
            statusCheckerTask.handleTimeout(t);
        }
        if (exit) {
            statusCheckerTask.handleExit(t);
        }
    }

    protected boolean exitPolling(T t) {
        return false;
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