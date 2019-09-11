package com.sequenceiq.environment.credential.exception;

public class CredentialVerificationException extends RuntimeException {

    public CredentialVerificationException(String message) {
        super(message);
    }

    public CredentialVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
