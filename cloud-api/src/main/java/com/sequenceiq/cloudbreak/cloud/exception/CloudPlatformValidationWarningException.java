package com.sequenceiq.cloudbreak.cloud.exception;

public class CloudPlatformValidationWarningException extends RuntimeException {

    public CloudPlatformValidationWarningException(String message) {
        super(message);
    }

    public CloudPlatformValidationWarningException(String message, Throwable cause) {
        super(message, cause);
    }
}
