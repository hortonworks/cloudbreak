package com.sequenceiq.environment.network.v1.converter;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentNetworkConverter {

    BaseNetwork convert(NetworkDto source, Environment environment);

    BaseNetwork convert(EnvironmentDto environmentDto);

    EnvironmentNetworkResponse convert(BaseNetwork source);

    CloudPlatform getCloudPlatform();

    NetworkDto convertToDto(BaseNetwork source);
}
