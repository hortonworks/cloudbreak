package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

public interface FreeIpaNetworkProvider {

    NetworkRequest provider(Environment environment);

    String availabilityZone(NetworkRequest networkRequest, Environment environment);

    CloudPlatform cloudPlatform();
}
