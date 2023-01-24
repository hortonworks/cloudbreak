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

        Map<String, Object> attributes = buildAttributes(source, availabilityZone);

        try {
            result.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return result;
    }

    private Map<String, Object> buildAttributes(EnvironmentNetworkResponse source, String availabilityZone) {
        Map<String, Object> attributes = new HashMap<>();
        CloudSubnet cloudSubnet = getCloudSubnet(source, availabilityZone);
        LOGGER.debug("Chosen subnet: {}", cloudSubnet);

        attributes.put(SUBNET_ID, cloudSubnet.getId());
        attributes.put(CLOUD_PLATFORM, getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        if (PublicEndpointAccessGateway.ENABLED.equals(source.getPublicEndpointAccessGateway())) {
            attachEndpointGatewaySubnet(source, attributes, cloudSubnet);
        }
        return attributes;
    }

    /**
     * Attach the subnet ID in which to place a public endpoint gateway.
     *
     * The default implementation should be sufficient for GCP and AWS.
     *
     * For Azure, we do not need to attach the endpoint gateway to a public subnet, so this method must be overrridable.
     * @param source contains source information to retrieve subnets from
     * @param attributes a Map which we put the gateway subnet ID in
     */
    protected void attachEndpointGatewaySubnet(EnvironmentNetworkResponse source, Map<String, Object> attributes, CloudSubnet cloudSubnet) {
        Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(source, cloudSubnet.getId());
        if (endpointGatewaySubnet.isPresent()) {
            attributes.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaySubnet.get().getId());
        } else {
            throw new BadRequestException("Could not find public subnet in availability zone: " + cloudSubnet.getAvailabilityZone());
        }
    }

    private CloudSubnet getCloudSubnet(EnvironmentNetworkResponse source, String availabilityZone) {
        Optional<CloudSubnet> cloudSubnet = subnetSelector.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(),
                availabilityZone, SelectionFallbackStrategy.ALLOW_FALLBACK);
        return cloudSubnet.orElseThrow(() -> new BadRequestException("No subnet for the given availability zone: " + availabilityZone));
    }

    abstract Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source);
}
