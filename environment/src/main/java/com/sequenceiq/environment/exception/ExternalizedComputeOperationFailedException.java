package com.sequenceiq.environment.exception;

public class ExternalizedComputeOperationFailedException extends RuntimeException {

    public ExternalizedComputeOperationFailedException(String message) {
        super(message);
    }

    public ExternalizedComputeOperationFailedException(String message, Exception cause) {
        super(message, cause);
    }
}
