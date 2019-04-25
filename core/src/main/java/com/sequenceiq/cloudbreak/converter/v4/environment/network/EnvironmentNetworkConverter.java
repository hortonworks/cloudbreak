package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.domain.environment.Environment;

public interface EnvironmentNetworkConverter {

    BaseNetwork convert(EnvironmentNetworkV4Request source, Environment environment);

    EnvironmentNetworkV4Response convert(BaseNetwork source);

    Network convertToLegacyNetwork(BaseNetwork source);

    CloudPlatform getCloudPlatform();
}
