package com.sequenceiq.cloudbreak.cloud.model.encryption;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class UnexpectedKeyTypeException extends CloudConnectorException {

    public UnexpectedKeyTypeException(String message) {
        super(message);
    }

    public UnexpectedKeyTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedKeyTypeException(Throwable cause) {
        super(cause);
    }

    protected UnexpectedKeyTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
