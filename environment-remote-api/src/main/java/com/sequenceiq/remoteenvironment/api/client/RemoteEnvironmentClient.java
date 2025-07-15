package com.sequenceiq.remoteenvironment.api.client;

import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;

public interface RemoteEnvironmentClient {
    RemoteEnvironmentEndpoint remoteEnvironmentEndpoint();

    RemoteEnvironmentV2Endpoint remoteEnvironmentV2Endpoint();
}
