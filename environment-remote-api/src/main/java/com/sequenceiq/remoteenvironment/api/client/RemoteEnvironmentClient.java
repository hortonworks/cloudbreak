package com.sequenceiq.remoteenvironment.api.client;

import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

public interface RemoteEnvironmentClient {
    RemoteEnvironmentEndpoint remoteEnvironmentEndpoint();
}
