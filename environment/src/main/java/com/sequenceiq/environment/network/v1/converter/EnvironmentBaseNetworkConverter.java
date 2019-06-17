package com.sequenceiq.environment.network.v1.converter;

import java.util.Set;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Override
    public BaseNetwork convert(NetworkDto network, Environment environment) {
        BaseNetwork result = createProviderSpecificNetwork(network);
        result.setName(environment.getName());
        EnvironmentView environmentView = convertEnvToView(environment);
        result.setEnvironments(Set.of(environmentView));
        result.setSubnetIds(network.getSubnetIds());
        result.setSubnetMetas(network.getSubnetMetas());
        result.setResourceCrn(network.getResourceCrn());
        return result;
    }

    @Override
    public BaseNetwork convert(EnvironmentDto environmentDto) {
        NetworkDto network = environmentDto.getNetwork();
        BaseNetwork result = createProviderSpecificNetwork(network);
        result.setName(environmentDto.getName());
        EnvironmentView environmentView = convertEnvironmentDtoToEnvironmentView(environmentDto);
        environmentView.setNetwork(result);
        result.setEnvironments(Set.of(environmentView));
        result.setSubnetIds(network.getSubnetIds());
        result.setSubnetMetas(network.getSubnetMetas());
        result.setResourceCrn(network.getResourceCrn());
        return result;
    }

    @Override
    public EnvironmentNetworkResponse convert(BaseNetwork source) {
        EnvironmentNetworkResponse result = new EnvironmentNetworkResponse();
        result.setCrn(source.getResourceCrn());
        result.setName(source.getName());
        result.setSubnetIds(source.getSubnetIdsSet());
        result.setSubnetMetas(source.getSubnetMetasMap());
        result = setProviderSpecificFields(result, source);
        return result;
    }

    @Override
    public NetworkDto convertToDto(BaseNetwork source) {
        EnvironmentNetworkResponse result = new EnvironmentNetworkResponse();
        result.setCrn(source.getResourceCrn());
        result.setName(source.getName());
        result.setSubnetIds(source.getSubnetIdsSet());
        result.setSubnetMetas(source.getSubnetMetasMap());
        NetworkDto.Builder builder = NetworkDto.Builder.aNetworkDto()
                .withId(source.getId())
                .withName(source.getName())
                .withSubnetIds(source.getSubnetIdsSet())
                .withSubnetMetas(source.getSubnetMetasMap())
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

    private EnvironmentView convertEnvironmentDtoToEnvironmentView(EnvironmentDto environmentDto) {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setId(environmentDto.getId());
        environmentView.setAccountId(environmentDto.getAccountId());
        environmentView.setDescription(environmentDto.getDescription());
        environmentView.setCloudPlatform(environmentDto.getCloudPlatform());
        environmentView.setCredential(environmentDto.getCredential());
        environmentView.setLatitude(environmentDto.getLocation().getLatitude());
        environmentView.setLongitude(environmentDto.getLocation().getLongitude());
        environmentView.setLocation(environmentDto.getLocation().getName());
        environmentView.setLocationDisplayName(environmentDto.getLocation().getDisplayName());
        environmentView.setRegions(environmentDto.getRegions());
        environmentView.setName(environmentDto.getName());
        return environmentView;
    }

    abstract BaseNetwork createProviderSpecificNetwork(NetworkDto network);

    abstract EnvironmentNetworkResponse setProviderSpecificFields(EnvironmentNetworkResponse result, BaseNetwork source);

    abstract NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork source);
}
