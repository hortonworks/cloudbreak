package com.sequenceiq.environment.environment.converter.network;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

public interface EnvironmentNetworkConverter {

    BaseNetwork convert(EnvironmentNetworkV1Request source, Environment environment);

    EnvironmentNetworkV1Response convert(BaseNetwork source);

    CloudPlatform getCloudPlatform();
}
