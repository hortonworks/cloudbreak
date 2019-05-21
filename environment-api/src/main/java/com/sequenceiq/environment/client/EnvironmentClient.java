package com.sequenceiq.environment.client;

import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.environment.v1.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.proxy.endpoint.ProxyV1Endpoint;

public interface EnvironmentClient {
    CredentialEndpoint credentialV1Endpoint();

    ProxyV1Endpoint proxyV1Endpoint();

    EnvironmentEndpoint environmentV1Endpoint();
}