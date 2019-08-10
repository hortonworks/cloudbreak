package com.sequenceiq.environment.network.v1.converter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.v1.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Inject
    private EnvironmentViewConverter environmentViewConverter;

    @Override
    public BaseNetwork convert(Environment environment, NetworkDto creationDto, Map<String, CloudSubnet> subnetMetas) {
        BaseNetwork result = createProviderSpecificNetwork(creationDto);
        result.setName(creationDto.getNetworkName() != null ? creationDto.getNetworkName() : environment.getName());
        result.setNetworkCidr(creationDto.getNetworkCidr());
        result.setEnvironments(convertEnvToView(environment));
        setRegistrationType(result, creationDto);
        result.setSubnetIds(creationDto.getSubnetIds());
        result.setSubnetMetas(subnetMetas);
        return result;
    }

    @Override
    public NetworkDto convertToDto(BaseNetwork source) {
        NetworkDto.Builder builder = NetworkDto.Builder.aNetworkDto()
                .withId(source.getId())
                .withName(source.getName())
                .withSubnetIds(source.getSubnetIds())
                .withSubnetMetas(source.getSubnetMetas())
                .withNetworkCidr(source.getNetworkCidr())
                .withResourceCrn(source.getResourceCrn())
                .withNetworkId(source.getNetworkId());
        return setProviderSpecificFields(builder, source);
    }

    private Set<EnvironmentView> convertEnvToView(Environment environment) {
        return Collections.singleton(environmentViewConverter.convert(environment));
    }

    abstract BaseNetwork createProviderSpecificNetwork(NetworkDto network);

    abstract NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork source);

    abstract void setRegistrationType(BaseNetwork result, NetworkDto networkDto);
}
