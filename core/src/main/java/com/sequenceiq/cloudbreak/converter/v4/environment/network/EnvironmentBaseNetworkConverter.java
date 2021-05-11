package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.CLOUD_PLATFORM;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseNetworkConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private SubnetSelector subnetSelector;

    @Override
    public Network convertToLegacyNetwork(EnvironmentNetworkResponse source, String availabilityZone) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);
        result.setOutboundInternetTraffic(source.getOutboundInternetTraffic());
        result.setNetworkCidrs(source.getNetworkCidrs());
        Map<String, Object> attributes = new HashMap<>();
        Optional<CloudSubnet> cloudSubnet = subnetSelector.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(),
            availabilityZone, SelectionFallbackStrategy.ALLOW_FALLBACK);
        if (cloudSubnet.isEmpty()) {
            throw new BadRequestException("No subnet for the given availability zone: " + availabilityZone);
        }
        LOGGER.debug("Chosen subnet: {}", cloudSubnet.get());

        attributes.put(SUBNET_ID, cloudSubnet.get().getId());
        attributes.put(CLOUD_PLATFORM, getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        if (PublicEndpointAccessGateway.ENABLED.equals(source.getPublicEndpointAccessGateway())) {
            Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(source, cloudSubnet.get().getId());
            if (endpointGatewaySubnet.isPresent()) {
                attributes.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaySubnet.get().getId());
            } else {
                throw new BadRequestException("Could not find public subnet in availability zone: " + cloudSubnet.get().getAvailabilityZone());
            }
        }

        try {
            result.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return result;
    }

    abstract Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source);
}
