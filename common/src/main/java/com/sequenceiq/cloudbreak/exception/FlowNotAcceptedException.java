package com.sequenceiq.cloudbreak.exception;

public class FlowNotAcceptedException extends RuntimeException {

    public FlowNotAcceptedException(String message) {
        super(message);
    }

    public FlowNotAcceptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
