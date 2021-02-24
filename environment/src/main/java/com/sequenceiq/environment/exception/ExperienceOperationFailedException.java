package com.sequenceiq.environment.exception;

public class ExperienceOperationFailedException extends RuntimeException {

    public ExperienceOperationFailedException(String message) {
        super(message);
    }

    public ExperienceOperationFailedException(Throwable cause) {
        super(cause);
    }

    public ExperienceOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
