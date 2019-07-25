package com.sequenceiq.cloudbreak.auth.altus.exception;

public class UmsOperationException extends RuntimeException {

    public UmsOperationException(String message) {
        super(message);
    }

    public UmsOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
