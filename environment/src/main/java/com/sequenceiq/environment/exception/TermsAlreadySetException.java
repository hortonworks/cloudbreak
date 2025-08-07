package com.sequenceiq.environment.exception;

public class TermsAlreadySetException extends RuntimeException {
    public TermsAlreadySetException(String message, Throwable cause) {
        super(message, cause);
    }
}
