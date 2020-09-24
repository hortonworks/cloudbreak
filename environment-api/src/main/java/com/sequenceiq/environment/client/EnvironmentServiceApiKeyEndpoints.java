package com.sequenceiq.environment.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.AuditCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public class EnvironmentServiceApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements EnvironmentClient {

    protected EnvironmentServiceApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public CredentialEndpoint credentialV1Endpoint() {
        return getEndpoint(CredentialEndpoint.class);
    }

    @Override
    public AuditCredentialEndpoint auditCredentialV1Endpoint() {
        return getEndpoint(AuditCredentialEndpoint.class);
    }

    @Override
    public ProxyEndpoint proxyV1Endpoint() {
        return getEndpoint(ProxyEndpoint.class);
    }

    @Override
    public EnvironmentEndpoint environmentV1Endpoint() {
        return getEndpoint(EnvironmentEndpoint.class);
    }

    @Override
    public FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }

    @Override
    public CDPStructuredEventV1Endpoint structuredEventsV1Endpoint() {
        return getEndpoint(CDPStructuredEventV1Endpoint.class);
    }
}
