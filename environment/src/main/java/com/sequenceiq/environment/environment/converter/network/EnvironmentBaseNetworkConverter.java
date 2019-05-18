package com.sequenceiq.environment.environment.converter.network;

import java.util.Set;

import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Override
    public BaseNetwork convert(EnvironmentNetworkV1Request source, Environment environment) {
        BaseNetwork result = createProviderSpecificNetwork(source);
        result.setName(environment.getName());
        EnvironmentView environmentView = convertEnvToView(environment);
        result.setEnvironments(Set.of(environmentView));
        result.setSubnetIds(source.getSubnetIds());
        return result;
    }

    @Override
    public EnvironmentNetworkV1Response convert(BaseNetwork source) {
        EnvironmentNetworkV1Response result = new EnvironmentNetworkV1Response();
        result.setId(source.getId());
        result.setName(source.getName());
        result.setSubnetIds(source.getSubnetIdsSet());
        result = setProviderSpecificFields(result, source);
        return result;
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

    abstract BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV1Request source);

    abstract EnvironmentNetworkV1Response setProviderSpecificFields(EnvironmentNetworkV1Response result, BaseNetwork source);
}
