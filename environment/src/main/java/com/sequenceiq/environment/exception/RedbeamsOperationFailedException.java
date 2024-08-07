package com.sequenceiq.environment.exception;

public class RedbeamsOperationFailedException extends RuntimeException {

    public RedbeamsOperationFailedException(String message) {
        super(message);
    }

    public RedbeamsOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
