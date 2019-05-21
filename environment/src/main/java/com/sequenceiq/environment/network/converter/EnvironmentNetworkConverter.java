package com.sequenceiq.environment.network.converter;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.v1.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentNetworkConverter {

    BaseNetwork convert(NetworkDto source, Environment environment);

    EnvironmentNetworkResponse convert(BaseNetwork source);

    CloudPlatform getCloudPlatform();

    NetworkDto convertToDto(BaseNetwork source);
}
