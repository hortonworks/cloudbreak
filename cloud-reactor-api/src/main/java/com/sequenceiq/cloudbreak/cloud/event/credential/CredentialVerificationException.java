package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

public class CredentialVerificationException extends BadRequestException {

    public CredentialVerificationException(String message) {
        super(message);
    }

    public CredentialVerificationException(String message, Exception errorDetails) {
        super(message, errorDetails);
    }
}
