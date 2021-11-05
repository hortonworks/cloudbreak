package com.sequenceiq.cloudbreak.exception;

public class FlowsAlreadyRunningException extends CloudbreakApiException {

    public FlowsAlreadyRunningException(String message) {
        super(message);
    }

    public FlowsAlreadyRunningException(String message, Throwable cause) {
        super(message, cause);
    }
}
