package com.sequenceiq.cloudbreak.service.stack.connector;

public class CloudResourceOperationFailedException extends RuntimeException {

    public CloudResourceOperationFailedException(String message) {
        super(message);
    }

    public CloudResourceOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudResourceOperationFailedException(Throwable cause) {
        super(cause);
    }

    protected CloudResourceOperationFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
