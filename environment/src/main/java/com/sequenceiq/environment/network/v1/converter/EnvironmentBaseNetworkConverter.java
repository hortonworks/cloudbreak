package com.sequenceiq.environment.network.v1.converter;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Override
    public BaseNetwork convert(Environment environment, NetworkDto creationDto) {
        BaseNetwork result = createProviderSpecificNetwork(creationDto);
        result.setName(creationDto.getNetworkName() != null ? creationDto.getNetworkName() : environment.getName());
        result.setNetworkCidr(creationDto.getNetworkCidr());
        result.setEnvironments(convertEnvToView(environment, result));
        result.setRegistrationType(RegistrationType.EXISTING);
        result.setSubnetIds(creationDto.getSubnetIds());
        result.setSubnetMetas(creationDto.getSubnetIds().stream().collect(Collectors.toMap(Function.identity(), id -> new CloudSubnet(id, null))));
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
        result.setAccountId(environmentDto.getAccountId());
        return result;
    }

    @Override
    public EnvironmentNetworkResponse convert(BaseNetwork source) {
        EnvironmentNetworkResponse result = new EnvironmentNetworkResponse();
        result.setCrn(source.getResourceCrn());
        result.setName(source.getName());
        result.setNetworkCidr(source.getNetworkCidr());
        result.setSubnetIds(source.getSubnetMetasMap().keySet());
        result.setSubnetMetas(source.getSubnetMetasMap());
        result = setProviderSpecificFields(result, source);
        return result;
    }

    @Override
    public NetworkDto convertToDto(BaseNetwork source) {
        NetworkDto.Builder builder = NetworkDto.Builder.aNetworkDto()
                .withId(source.getId())
                .withName(source.getName())
                .withName(source.getName())
                .withNetworkCidr(source.getNetworkCidr())
                .withSubnetIds(source.getSubnetMetasMap().keySet())
                .withSubnetMetas(source.getSubnetMetasMap())
                .withNetworkCidr(source.getNetworkCidr())
                .withResourceCrn(source.getResourceCrn());
        return setProviderSpecificFields(builder, source);
    }

    private Set<EnvironmentView> convertEnvToView(Environment environment, BaseNetwork baseNetwork) {
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
        return Collections.singleton(environmentView);
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
