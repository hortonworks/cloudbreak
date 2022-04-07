package com.sequenceiq.freeipa.util;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ThreadInterruptChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadInterruptChecker.class);

    public void throwTimeoutExIfInterrupted() throws TimeoutException {
        if (Thread.interrupted()) {
            LOGGER.warn("Thread is interrupted");
            throw new TimeoutException("Thread is interrupted");
        }
    }
}
