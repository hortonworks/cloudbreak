package com.sequenceiq.environment.exception;

public class ExternalizedComputeValidationFailedException extends Exception  {

    public ExternalizedComputeValidationFailedException(String message) {
        super(message);
    }

    public ExternalizedComputeValidationFailedException(String message, Exception cause) {
        super(message, cause);
    }
}
