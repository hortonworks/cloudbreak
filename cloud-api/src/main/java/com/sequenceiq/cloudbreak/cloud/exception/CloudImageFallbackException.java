package com.sequenceiq.cloudbreak.cloud.exception;

public class CloudImageFallbackException extends CloudConnectorException {

    public CloudImageFallbackException(String message) {
        super(message);
    }

    public CloudImageFallbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudImageFallbackException(Throwable cause) {
        super(cause);
    }
}
