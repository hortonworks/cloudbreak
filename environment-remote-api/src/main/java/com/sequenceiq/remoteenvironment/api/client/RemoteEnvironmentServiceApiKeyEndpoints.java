package com.sequenceiq.remoteenvironment.api.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;

public class RemoteEnvironmentServiceApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements RemoteEnvironmentClient {

    protected RemoteEnvironmentServiceApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public RemoteEnvironmentEndpoint remoteEnvironmentEndpoint() {
        return getEndpoint(RemoteEnvironmentEndpoint.class);
    }

    @Override
    public RemoteEnvironmentV2Endpoint remoteEnvironmentV2Endpoint() {
        return getEndpoint(RemoteEnvironmentV2Endpoint.class);
    }
}
