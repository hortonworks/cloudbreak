package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

public interface CredentialAwareEnvV4Request {

    String getCredentialName();

    void setCredentialName(String credentialName);

    CredentialV4Request getCredential();

    void setCredential(CredentialV4Request credential);
}
