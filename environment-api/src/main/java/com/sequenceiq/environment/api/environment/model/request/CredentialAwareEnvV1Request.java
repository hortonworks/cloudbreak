package com.sequenceiq.environment.api.environment.model.request;

import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;

public interface CredentialAwareEnvV1Request {

    String getCredentialName();

    void setCredentialName(String credentialName);

    CredentialRequest getCredential();

    void setCredential(CredentialRequest credential);
}
