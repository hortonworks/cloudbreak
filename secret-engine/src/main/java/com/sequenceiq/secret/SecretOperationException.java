package com.sequenceiq.secret;

public class SecretOperationException extends RuntimeException {

    public SecretOperationException() {
    }

    public SecretOperationException(String message) {
        super(message);
    }

    public SecretOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretOperationException(Throwable cause) {
        super(cause);
    }

    public SecretOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
