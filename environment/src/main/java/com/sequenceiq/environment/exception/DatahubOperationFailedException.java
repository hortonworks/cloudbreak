package com.sequenceiq.environment.exception;

public class DatahubOperationFailedException extends RuntimeException {

    public DatahubOperationFailedException(String message) {
        super(message);
    }

    public DatahubOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
