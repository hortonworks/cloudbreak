package com.sequenceiq.cloudbreak.service;

public class OperationException extends RuntimeException {

    public OperationException(Throwable cause) {
        super(cause);
    }

    public OperationException(String message) {
        super(message);
    }
}
