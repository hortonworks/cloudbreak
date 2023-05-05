package com.sequenceiq.cloudbreak.cloud.exception;

public class InsufficientCapacityException extends CloudConnectorException {

    public InsufficientCapacityException(String message) {
        super(message);
    }

    public InsufficientCapacityException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientCapacityException(Throwable cause) {
        super(cause);
    }

    protected InsufficientCapacityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
