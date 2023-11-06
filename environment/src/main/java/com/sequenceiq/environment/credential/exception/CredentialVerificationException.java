package com.sequenceiq.environment.credential.exception;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

public class CredentialVerificationException extends BadRequestException {

    public CredentialVerificationException(String message) {
        super(message);
    }

    public CredentialVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
