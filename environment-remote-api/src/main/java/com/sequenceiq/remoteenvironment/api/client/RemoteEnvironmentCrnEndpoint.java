package com.sequenceiq.remoteenvironment.api.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;

public class RemoteEnvironmentCrnEndpoint extends AbstractUserCrnServiceEndpoint implements RemoteEnvironmentClient {

    public RemoteEnvironmentCrnEndpoint(WebTarget webTarget, String crn) {
        super(webTarget, crn);
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
