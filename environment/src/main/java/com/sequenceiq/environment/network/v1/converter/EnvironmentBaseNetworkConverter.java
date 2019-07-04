package com.sequenceiq.environment.network.v1.converter;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Override
    public BaseNetwork convert(Environment environment, NetworkDto creationDto) {
        BaseNetwork result = createProviderSpecificNetwork(creationDto);
        result.setName(creationDto.getNetworkName() != null ? creationDto.getNetworkName() : environment.getName());
        result.setNetworkCidr(creationDto.getNetworkCidr());
        result.setEnvironments(convertEnvToView(environment));
        setRegistrationType(result, creationDto);
        result.setSubnetIds(creationDto.getSubnetIds());
        result.setSubnetMetas(creationDto.getSubnetIds().stream().collect(Collectors.toMap(Function.identity(), id -> new CloudSubnet(id, null))));
        return result;
    }

    @Override
    public NetworkDto convertToDto(BaseNetwork source) {
        NetworkDto.Builder builder = NetworkDto.Builder.aNetworkDto()
                .withId(source.getId())
                .withName(source.getName())
                .withSubnetIds(source.getSubnetIdsSet())
                .withSubnetMetas(source.getSubnetMetasMap())
                .withNetworkCidr(source.getNetworkCidr())
                .withResourceCrn(source.getResourceCrn());
        return setProviderSpecificFields(builder, source);
    }

    private Set<EnvironmentView> convertEnvToView(Environment environment) {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setId(environment.getId());
        environmentView.setName(environment.getName());
        environmentView.setAccountId(environment.getAccountId());
        environmentView.setDescription(environment.getDescription());
        environmentView.setCloudPlatform(environment.getCloudPlatform());
        environmentView.setCredential(environment.getCredential());
        environmentView.setLatitude(environment.getLatitude());
        environmentView.setLongitude(environment.getLongitude());
        environmentView.setLocation(environment.getLocation());
        environmentView.setLocationDisplayName(environment.getLocationDisplayName());
        environmentView.setNetwork(environment.getNetwork());
        environmentView.setRegions(environment.getRegions());
        environmentView.setTelemetry(environment.getTelemetry());
        return Collections.singleton(environmentView);
    }

    abstract BaseNetwork createProviderSpecificNetwork(NetworkDto network);

    abstract NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork source);

    abstract void setRegistrationType(BaseNetwork result, NetworkDto networkDto);
}
