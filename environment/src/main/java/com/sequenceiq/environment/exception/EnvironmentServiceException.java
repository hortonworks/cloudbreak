package com.sequenceiq.environment.exception;

public class EnvironmentServiceException extends RuntimeException {

    public EnvironmentServiceException(String message) {
        super(message);
    }

    public EnvironmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnvironmentServiceException(Throwable cause) {
        super(cause);
    }

    public EnvironmentServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
