package com.sequenceiq.cloudbreak.cloud.connector;

/**
 * Base for cloud provider specific errors.
 * Intended to replace the InternalServerException.
 */
public class CloudConnectorException extends RuntimeException {

    public CloudConnectorException(String message) {
        super(message);
    }

    public CloudConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudConnectorException(Throwable cause) {
        super(cause);
    }

    protected CloudConnectorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
