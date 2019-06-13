package com.sequenceiq.environment.api.v1.environment.model.request;

public interface CredentialAwareEnvRequest {

    String getCredentialName();

    void setCredentialName(String credentialName);
}
