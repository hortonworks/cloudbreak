package com.sequenceiq.it.cloudbreak.util.wait.service;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public abstract class ExceptionChecker<T> implements StatusChecker<T> {

    @Override
    public void handleException(Exception e) {
        throw new TestFailException(e.getMessage());
    }

}
