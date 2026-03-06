package com.sequenceiq.environment.exception;

public class StackOperationFailedException extends RuntimeException {
    public StackOperationFailedException(String message) {
        super(message);
    }

    public StackOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
