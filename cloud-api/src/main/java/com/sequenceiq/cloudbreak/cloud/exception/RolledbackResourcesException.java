package com.sequenceiq.cloudbreak.cloud.exception;

public class RolledbackResourcesException extends CloudConnectorException {

    public RolledbackResourcesException(String message) {
        super(message);
    }

    public RolledbackResourcesException(String message, Throwable cause) {
        super(message, cause);
    }

    public RolledbackResourcesException(Throwable cause) {
        super(cause);
    }

    protected RolledbackResourcesException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
