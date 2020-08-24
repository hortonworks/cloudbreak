package com.sequenceiq.environment.client;

import com.sequenceiq.environment.api.v1.credential.endpoint.AuditCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public interface EnvironmentClient {
    CredentialEndpoint credentialV1Endpoint();

    AuditCredentialEndpoint auditCredentialV1Endpoint();

    ProxyEndpoint proxyV1Endpoint();

    EnvironmentEndpoint environmentV1Endpoint();

    FlowEndpoint flowEndpoint();

    FlowPublicEndpoint flowPublicEndpoint();

}