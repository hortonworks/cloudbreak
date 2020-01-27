package com.sequenceiq.environment.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;

public class EnvironmentServiceApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements EnvironmentClient {

    protected EnvironmentServiceApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public CredentialEndpoint credentialV1Endpoint() {
        return getEndpoint(CredentialEndpoint.class);
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
}
