package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;

public interface CredentialAwareEnvV4Request {

    String getCredentialName();

    void setCredentialName(String credentialName);

    CredentialRequest getCredential();

    void setCredential(CredentialRequest credential);
}
