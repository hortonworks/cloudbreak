package com.sequenceiq.remoteenvironment.api.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

public class RemoteEnvironmentCrnEndpoint extends AbstractUserCrnServiceEndpoint implements RemoteEnvironmentClient {

    public RemoteEnvironmentCrnEndpoint(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }

    @Override
    public RemoteEnvironmentEndpoint remoteEnvironmentEndpoint() {
        return getEndpoint(RemoteEnvironmentEndpoint.class);
    }
}
