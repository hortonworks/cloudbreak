package com.sequenceiq.environment.network.converter;

import java.util.Set;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.network.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Override
    public BaseNetwork convert(NetworkDto network, Environment environment) {
        BaseNetwork result = createProviderSpecificNetwork(network);
        result.setName(environment.getName());
        EnvironmentView environmentView = convertEnvToView(environment);
        result.setEnvironments(Set.of(environmentView));
        result.setSubnetIds(network.getSubnetIds());
        return result;
    }

    @Override
    public EnvironmentNetworkResponse convert(BaseNetwork source) {
        EnvironmentNetworkResponse result = new EnvironmentNetworkResponse();
        result.setId(source.getResourceCrn());
        result.setName(source.getName());
        result.setSubnetIds(source.getSubnetIdsSet());
        result = setProviderSpecificFields(result, source);
        return result;
    }

    @Override
    public NetworkDto convertToDto(BaseNetwork source) {
        EnvironmentNetworkResponse result = new EnvironmentNetworkResponse();
        result.setId(source.getResourceCrn());
        result.setName(source.getName());
        result.setSubnetIds(source.getSubnetIdsSet());
        NetworkDto.NetworkDtoBuilder builder = NetworkDto.NetworkDtoBuilder.aNetworkDto()
                .withId(source.getId())
                .withSubnetIds(source.getSubnetIdsSet())
                .withResourceCrn(source.getResourceCrn());
        return setProviderSpecificFields(builder, source);
    }

    private EnvironmentView convertEnvToView(Environment environment) {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setId(environment.getId());
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
        environmentView.setName(environment.getName());
        return environmentView;
    }

    abstract BaseNetwork createProviderSpecificNetwork(NetworkDto network);

    abstract EnvironmentNetworkResponse setProviderSpecificFields(EnvironmentNetworkResponse result, BaseNetwork source);

    abstract NetworkDto setProviderSpecificFields(NetworkDto.NetworkDtoBuilder builder, BaseNetwork source);
}
