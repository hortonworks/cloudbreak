package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupNetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

public interface FreeIpaNetworkProvider {

    NetworkRequest network(EnvironmentDto environment, boolean multiAzRequired);

    default InstanceGroupNetworkRequest networkByGroup(EnvironmentDto environment) {
        return null;
    }

    String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment);

    default String availabilityZone(InstanceGroupNetworkRequest networkRequest, EnvironmentDto environment) {
        return null;
    }

    Set<String> subnets(NetworkRequest networkRequest);

    CloudPlatform cloudPlatform();
}
