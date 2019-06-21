package com.sequenceiq.environment.network.v1.converter;

import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentNetworkConverter {

    BaseNetwork convert(Environment environment, NetworkDto creationDto);

    BaseNetwork setProviderSpecificNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork);

    CloudPlatform getCloudPlatform();

    NetworkDto convertToDto(BaseNetwork source);

    boolean hasExistingNetwork(BaseNetwork baseNetwork);
}
