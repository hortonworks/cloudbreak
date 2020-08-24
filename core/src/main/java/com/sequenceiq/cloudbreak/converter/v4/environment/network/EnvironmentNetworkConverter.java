package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import org.springframework.data.util.Pair;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public interface EnvironmentNetworkConverter {

    Network convertToLegacyNetwork(EnvironmentNetworkResponse source);

    Pair<InstanceGroupNetwork, String> convertToLegacyInstanceGroupNetwork(EnvironmentNetworkResponse source, String availabilityZone);

    CloudPlatform getCloudPlatform();
}
