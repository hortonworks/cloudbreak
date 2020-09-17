package com.sequenceiq.cloudbreak.cloud.model.credential;

public class CredentialVerificationContext {
    private final Boolean creationVerification;

    public CredentialVerificationContext(Boolean creationVerification) {
        this.creationVerification = creationVerification;
    }

    public Boolean getCreationVerification() {
        return creationVerification;
    }
}
