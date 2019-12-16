package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
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
    public Network convertToLegacyNetwork(EnvironmentNetworkResponse source, String availabilityZone) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);
        Map<String, Object> attributes = new HashMap<>();
        Optional<CloudSubnet> cloudSubnet;
        if (StringUtils.isNotBlank(source.getPreferedSubnetId())) {
            LOGGER.debug("Choosing subnet by prefered subnet Id {}", source.getPreferedSubnetId());
            cloudSubnet = Optional.of(source.getSubnetMetas().get(source.getPreferedSubnetId()));
        } else if (StringUtils.isNotEmpty(availabilityZone)) {
            LOGGER.debug("Choosing subnet by availability zone {}", availabilityZone);
            cloudSubnet = source.getSubnetMetas().values().stream()
                    .filter(s -> StringUtils.isNotEmpty(s.getAvailabilityZone()) &&
                            s.getAvailabilityZone().equals(availabilityZone))
                    .findFirst();
        } else {
            LOGGER.debug("Fallback to choose random subnet");
            cloudSubnet = source.getSubnetMetas().values().stream().findFirst();
        }
        if (cloudSubnet.isEmpty()) {
            throw new BadRequestException("No subnet for the given availability zone: " + availabilityZone);
        }
        LOGGER.debug("Chosen subnet: {}", cloudSubnet.get());
        attributes.put("subnetId", cloudSubnet.get().getId());
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
