package com.sequenceiq.cloudbreak.exception;

public class FlowNotAcceptedException extends CloudbreakApiException {

    public FlowNotAcceptedException(String message) {
        super(message);
    }

    public FlowNotAcceptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
