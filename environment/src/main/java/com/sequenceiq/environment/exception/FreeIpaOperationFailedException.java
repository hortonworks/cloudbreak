package com.sequenceiq.environment.exception;

public class FreeIpaOperationFailedException extends RuntimeException {

    public FreeIpaOperationFailedException(String message) {
        super(message);
    }

    public FreeIpaOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
