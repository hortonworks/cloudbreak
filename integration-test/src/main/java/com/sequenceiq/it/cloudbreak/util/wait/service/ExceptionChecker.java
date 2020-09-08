package com.sequenceiq.it.cloudbreak.util.wait.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public abstract class ExceptionChecker<T> implements StatusChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionChecker.class);

    @Override
    public void handleException(Exception e) {
        LOGGER.error("Failing with exception", e);
        throw new TestFailException(e.getMessage(), e);
    }

}
