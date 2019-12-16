package com.sequenceiq.cloudbreak.cloud.exception;

public class CloudUnauthorizedException extends CloudConnectorException {

    public CloudUnauthorizedException(String message) {
        super(message);
    }

    public CloudUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudUnauthorizedException(Throwable cause) {
        super(cause);
    }

    protected CloudUnauthorizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
