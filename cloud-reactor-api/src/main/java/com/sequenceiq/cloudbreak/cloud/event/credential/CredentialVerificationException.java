package com.sequenceiq.cloudbreak.cloud.event.credential;

public class CredentialVerificationException extends RuntimeException {

    public CredentialVerificationException(String message) {
        super(message);
    }

    public CredentialVerificationException(String message, Exception errorDetails) {
        super(message, errorDetails);
    }
}
