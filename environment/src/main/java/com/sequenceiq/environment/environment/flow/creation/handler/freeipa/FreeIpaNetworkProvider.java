package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

public interface FreeIpaNetworkProvider {

    NetworkRequest provider(EnvironmentDto environment);

    String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment);

    CloudPlatform cloudPlatform();
}
