package com.sequenceiq.environment.client;

import com.sequenceiq.environment.api.v1.credential.endpoint.EnvironmentCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;

public interface EnvironmentClient {
    CredentialEndpoint credentialV1Endpoint();

    ProxyEndpoint proxyV1Endpoint();

    EnvironmentEndpoint environmentV1Endpoint();

    EnvironmentCredentialEndpoint environmentCredentialV1Endpoint();

    FlowEndpoint flowEndpoint();
}