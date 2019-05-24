package com.sequenceiq.environment.client;

import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentV1Endpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;

public interface EnvironmentClient {
    CredentialEndpoint credentialV1Endpoint();

    ProxyEndpoint proxyV1Endpoint();

    EnvironmentV1Endpoint environmentV1Endpoint();
}