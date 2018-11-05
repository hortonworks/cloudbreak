package com.sequenceiq.cloudbreak.api.model.environment.request;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;

public interface CredentialAwareEnvRequest {

    String getCredentialName();

    void setCredentialName(String credentialName);

    CredentialRequest getCredential();

    void setCredential(CredentialRequest credential);
}
