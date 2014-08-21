package com.sequenceiq.cloudbreak.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PollingService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);

    public void pollWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, int interval, int maxAttempts) {
        boolean success = false;
        boolean timeout = false;
        int attempts = 0;
        while (!success && !timeout) {
            LOGGER.info("Polling attempt {}.", attempts);
            success = statusCheckerTask.checkStatus(t);
            if (success) {
                LOGGER.info(statusCheckerTask.successMessage(t));
                return;
            }
            sleep(interval);
            timeout = ++attempts >= maxAttempts;
        }
        if (timeout) {
            statusCheckerTask.handleTimeout(t);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted exception occured during polling.", e);
            Thread.currentThread().interrupt();
        }
    }

}