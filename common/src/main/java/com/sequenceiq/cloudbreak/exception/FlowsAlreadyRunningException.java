package com.sequenceiq.cloudbreak.exception;

public class FlowsAlreadyRunningException extends RuntimeException {

    public FlowsAlreadyRunningException(String message) {
        super(message);
    }

    public FlowsAlreadyRunningException(String message, Throwable cause) {
        super(message, cause);
    }
}
