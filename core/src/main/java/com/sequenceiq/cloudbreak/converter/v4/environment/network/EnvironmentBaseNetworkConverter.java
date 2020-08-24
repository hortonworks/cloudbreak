package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public abstract class EnvironmentBaseNetworkConverter implements EnvironmentNetworkConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseNetworkConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SubnetSelector subnetSelector;

    @Override
    public Network convertToLegacyNetwork(EnvironmentNetworkResponse source) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);
        result.setOutboundInternetTraffic(source.getOutboundInternetTraffic());
        result.setNetworkCidrs(source.getNetworkCidrs());
        Map<String, Object> attributes = new HashMap<>();
        Optional<CloudSubnet> cloudSubnet = subnetSelector.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(), availabilityZone, true);
        attributes.put("cloudPlatform", getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        try {
            result.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return result;
    }

    @Override
    public Pair<InstanceGroupNetwork, String> convertToLegacyInstanceGroupNetwork(EnvironmentNetworkResponse source, String availabilityZone) {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        Map<String, Object> attributes = new HashMap<>();
        Optional<CloudSubnet> cloudSubnet;
        if (StringUtils.isNotBlank(source.getPreferedSubnetId())) {
            LOGGER.debug("Choosing subnet by prefered subnet Id {}", source.getPreferedSubnetId());
            CloudSubnet cloudSubnetById = source.getSubnetMetas().get(source.getPreferedSubnetId());
            if (cloudSubnetById == null) {
                cloudSubnetById = source.getSubnetMetas().values()
                        .stream()
                        .filter(e -> e.getId().equals(source.getPreferedSubnetId()))
                        .findFirst().orElse(null);
            }
            cloudSubnet = Optional.of(cloudSubnetById);
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
        if (PublicEndpointAccessGateway.ENABLED.equals(source.getPublicEndpointAccessGateway())
            && entitlementService.publicEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            Optional<CloudSubnet> endpointGatewaySubnet = subnetSelector.chooseSubnetForEndpointGateway(source, cloudSubnet.get().getId());
            if (endpointGatewaySubnet.isPresent()) {
                attributes.put("endpointGatewaySubnetId", endpointGatewaySubnet.get().getId());
            } else {
                throw new BadRequestException("Could not find public subnet in availability zone: " + cloudSubnet.get().getAvailabilityZone());
            }
        }

        try {
            instanceGroupNetwork.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return Pair.of(instanceGroupNetwork, source.getSubnetMetas().get(cloudSubnet.get().getId()).getAvailabilityZone());
    }

    abstract Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source);
}
