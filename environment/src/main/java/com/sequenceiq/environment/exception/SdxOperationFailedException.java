package com.sequenceiq.environment.exception;

public class SdxOperationFailedException extends RuntimeException {

    public SdxOperationFailedException(String message) {
        super(message);
    }

    public SdxOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
