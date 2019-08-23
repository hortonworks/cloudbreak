package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public interface EnvironmentNetworkConverter {

    Network convertToLegacyNetwork(EnvironmentNetworkResponse source, String availabilityZone);

    CloudPlatform getCloudPlatform();
}
