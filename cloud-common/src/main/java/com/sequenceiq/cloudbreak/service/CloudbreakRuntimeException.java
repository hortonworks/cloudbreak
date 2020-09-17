package com.sequenceiq.cloudbreak.service;

public class CloudbreakRuntimeException extends RuntimeException {

    public CloudbreakRuntimeException(String message) {
        super(message);
    }

    public CloudbreakRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakRuntimeException(Throwable cause) {
        super(cause);
    }
}
