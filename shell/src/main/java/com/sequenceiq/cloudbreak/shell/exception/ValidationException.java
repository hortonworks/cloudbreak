package com.sequenceiq.cloudbreak.shell.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
