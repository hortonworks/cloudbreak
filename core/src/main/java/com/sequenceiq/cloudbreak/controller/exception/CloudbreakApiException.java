package com.sequenceiq.cloudbreak.controller.exception;

public class CloudbreakApiException extends RuntimeException {

    public CloudbreakApiException(String message) {
        super(message);
    }

    public CloudbreakApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
