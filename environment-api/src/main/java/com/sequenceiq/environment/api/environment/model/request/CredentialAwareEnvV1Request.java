package com.sequenceiq.environment.api.environment.model.request;

import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;

public interface CredentialAwareEnvV1Request {

    String getCredentialName();

    void setCredentialName(String credentialName);

    CredentialV1Request getCredential();

    void setCredential(CredentialV1Request credential);
}
