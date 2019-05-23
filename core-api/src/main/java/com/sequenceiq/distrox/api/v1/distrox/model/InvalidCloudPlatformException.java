package com.sequenceiq.distrox.api.v1.distrox.model;

public class InvalidCloudPlatformException extends RuntimeException {

    public InvalidCloudPlatformException() {
    }

    public InvalidCloudPlatformException(String message) {
        super(message);
    }

    public InvalidCloudPlatformException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCloudPlatformException(Throwable cause) {
        super(cause);
    }

    public InvalidCloudPlatformException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
