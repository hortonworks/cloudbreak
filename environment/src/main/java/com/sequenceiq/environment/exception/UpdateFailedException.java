package com.sequenceiq.environment.exception;

public class UpdateFailedException extends RuntimeException {

    public UpdateFailedException(String message) {
        super(message);
    }

    public UpdateFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
