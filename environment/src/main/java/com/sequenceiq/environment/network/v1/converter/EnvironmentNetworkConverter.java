package com.sequenceiq.environment.network.v1.converter;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentNetworkConverter {

    BaseNetwork convert(Environment environment, NetworkDto creationDto, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> gatewayEndpointSubnetMetas);

    BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork);

    CloudPlatform getCloudPlatform();

    NetworkDto convertToDto(BaseNetwork source);

    Network convertToNetwork(BaseNetwork baseNetwork);

    default NetworkDto.Builder extendBuilderWithProviderSpecificParameters(NetworkDto.Builder networkDtoBuilder, NetworkDto originalNetworkDto,
            NetworkDto newNetworkDto) {
        return networkDtoBuilder;
    }

    default void updateAvailabilityZones(BaseNetwork baseNetwork, Set<String> availabilityZones) {

    }

    default void updateProviderSpecificParameters(BaseNetwork baseNetwork, NetworkDto networkDto) {

    }

    default Set<String> getAvailabilityZones(BaseNetwork baseNetwork) {
        return Set.of();
    }
}
