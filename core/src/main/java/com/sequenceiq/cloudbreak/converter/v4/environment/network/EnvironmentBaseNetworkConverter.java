package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseNetworkConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public Network convertToLegacyNetwork(EnvironmentNetworkResponse source) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("subnetId", String.join(",", source.getSubnetIds()));
        attributes.put("cloudPlatform", getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        try {
            result.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return result;
    }

    abstract Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source);
}
