package com.sequenceiq.cloudbreak.exception;

public class CustomConfigsExistsException extends RuntimeException {
    public CustomConfigsExistsException(String message) {
        super(message);
    }
}
