package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseNetworkConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public BaseNetwork convert(EnvironmentNetworkV4Request source, Environment environment) {
        BaseNetwork result = createProviderSpecificNetwork(source);
        result.setName(environment.getName());
        result.setEnvironment(environment);
        result.setWorkspace(environment.getWorkspace());
        result.setSubnetIds(source.getSubnetIds());
        return result;
    }

    @Override
    public EnvironmentNetworkV4Response convert(BaseNetwork source) {
        EnvironmentNetworkV4Response result = new EnvironmentNetworkV4Response();
        result.setId(source.getId());
        result.setName(source.getName());
        result.setSubnetIds(source.getSubnetIdsSet());
        result = setProviderSpecificFields(result, source);
        return result;
    }

    @Override
    public Network convertToLegacyNetwork(BaseNetwork source) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("subnetId", String.join(",", source.getSubnetIdsSet()));
        attributes.put("cloudPlatform", getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        try {
            result.setAttributes(new Json(attributes));
        } catch (JsonProcessingException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return result;
    }

    abstract BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV4Request source);

    abstract EnvironmentNetworkV4Response setProviderSpecificFields(EnvironmentNetworkV4Response result, BaseNetwork source);

    abstract Map<String, Object> getAttributesForLegacyNetwork(BaseNetwork source);
}
