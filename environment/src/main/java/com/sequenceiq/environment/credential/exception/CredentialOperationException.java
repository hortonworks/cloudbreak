package com.sequenceiq.environment.credential.exception;

public class CredentialOperationException extends RuntimeException {

    public CredentialOperationException() {
    }

    public CredentialOperationException(String message) {
        super(message);
    }

    public CredentialOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CredentialOperationException(Throwable cause) {
        super(cause);
    }

    public CredentialOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
