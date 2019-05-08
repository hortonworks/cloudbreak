package com.sequenceiq.environment.api.environment.model.requests;

public interface CredentialAwareEnvRequest {

    String getCredentialName();

    void setCredentialName(String credentialName);

    CredentialV4Request getCredential();

    void setCredential(CredentialV4Request credential);
}
