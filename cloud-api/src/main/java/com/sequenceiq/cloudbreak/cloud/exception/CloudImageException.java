package com.sequenceiq.cloudbreak.cloud.exception;

public class CloudImageException extends CloudConnectorException {

    public CloudImageException(String message) {
        super(message);
    }

    public CloudImageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudImageException(Throwable cause) {
        super(cause);
    }
}
