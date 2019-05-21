package com.sequenceiq.environment.client;

import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.environment.endpoint.EnvironmentV1Endpoint;
import com.sequenceiq.environment.api.proxy.endpoint.ProxyV1Endpoint;

public interface EnvironmentClient {
    CredentialEndpoint credentialV1Endpoint();

    ProxyV1Endpoint proxyV1Endpoint();

    EnvironmentV1Endpoint environmentV1Endpoint();
}