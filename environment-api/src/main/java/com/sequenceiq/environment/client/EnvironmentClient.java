package com.sequenceiq.environment.client;

import com.sequenceiq.environment.api.credential.endpoint.CredentialV1Endpoint;
import com.sequenceiq.environment.api.environment.endpoint.EnvironmentV1Endpoint;
import com.sequenceiq.environment.api.proxy.endpoint.ProxyV1Endpoint;

public interface EnvironmentClient {
    CredentialV1Endpoint credentialV1Endpoint();

    ProxyV1Endpoint proxyV1Endpoint();

    EnvironmentV1Endpoint environmentV1Endpoint();
}