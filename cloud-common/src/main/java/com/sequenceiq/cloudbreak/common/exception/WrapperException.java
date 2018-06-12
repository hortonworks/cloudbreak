package com.sequenceiq.cloudbreak.common.exception;

public interface WrapperException {
    Throwable getRootCause();
}
