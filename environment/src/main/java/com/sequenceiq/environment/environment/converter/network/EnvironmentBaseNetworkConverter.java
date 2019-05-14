package com.sequenceiq.environment.environment.converter.network;

import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {

    @Override
    public BaseNetwork convert(EnvironmentNetworkV1Request source, Environment environment) {
        BaseNetwork result = createProviderSpecificNetwork(source);
        result.setName(environment.getName());
        result.setEnvironment(environment);
        result.setWorkspace(environment.getWorkspace());
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

    abstract BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV1Request source);

    abstract EnvironmentNetworkV1Response setProviderSpecificFields(EnvironmentNetworkV1Response result, BaseNetwork source);
}
