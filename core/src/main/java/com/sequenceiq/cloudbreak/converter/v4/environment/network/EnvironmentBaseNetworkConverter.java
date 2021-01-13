package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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
    private EntitlementService entitlementService;

    @Override
    public Network convertToLegacyNetwork(EnvironmentNetworkResponse source, String availabilityZone) {
        Network result = new Network();
        result.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        result.setSubnetCIDR(null);
        result.setOutboundInternetTraffic(source.getOutboundInternetTraffic());
        result.setNetworkCidrs(source.getNetworkCidrs());
        Map<String, Object> attributes = new HashMap<>();
        Optional<CloudSubnet> cloudSubnet = chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(), availabilityZone, true);
        if (cloudSubnet.isEmpty()) {
            throw new BadRequestException("No subnet for the given availability zone: " + availabilityZone);
        }
        LOGGER.debug("Chosen subnet: {}", cloudSubnet.get());

        attributes.put("subnetId", cloudSubnet.get().getId());
        attributes.put("cloudPlatform", getCloudPlatform().name());
        attributes.putAll(getAttributesForLegacyNetwork(source));
        if (entitlementService.publicEndpointAccessGatewayEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            chooseSubnetForEndpointGateway(source, cloudSubnet.get(), attributes);
        }

        try {
            result.setAttributes(new Json(attributes));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Environment's network could not be converted to network.", e);
        }
        return result;
    }

    private Optional<CloudSubnet> chooseSubnet(String preferredSubnetId, Map<String, CloudSubnet> subnetMetas,
            String availabilityZone, boolean allowRandom) {
        Optional<CloudSubnet> cloudSubnet;
        if (StringUtils.isNotEmpty(preferredSubnetId)) {
            LOGGER.debug("Choosing subnet by prefered subnet Id {}", preferredSubnetId);
            CloudSubnet cloudSubnetById = subnetMetas.get(preferredSubnetId);
            if (cloudSubnetById == null) {
                cloudSubnetById = subnetMetas.values()
                    .stream()
                    .filter(e -> e.getId().equals(preferredSubnetId))
                    .findFirst().orElse(null);
            }
            cloudSubnet = Optional.ofNullable(cloudSubnetById);
        } else if (StringUtils.isNotEmpty(availabilityZone)) {
            LOGGER.debug("Choosing subnet by availability zone {}", availabilityZone);
            cloudSubnet = subnetMetas.values().stream()
                .filter(s -> StringUtils.isNotEmpty(s.getAvailabilityZone()) &&
                    s.getAvailabilityZone().equals(availabilityZone))
                .findFirst();
        } else if (allowRandom) {
            LOGGER.debug("Fallback to choose random subnet");
            cloudSubnet = subnetMetas.values().stream().findFirst();
        } else {
            cloudSubnet = Optional.empty();
        }
        return cloudSubnet;
    }

    private void chooseSubnetForEndpointGateway(EnvironmentNetworkResponse source, CloudSubnet cloudSubnet, Map<String, Object> attributes) {
        if (source.getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED) {
            String selectedAZ = cloudSubnet.getAvailabilityZone();
            Map<String, CloudSubnet> subnetsToParse;
            if (source.getGatewayEndpointSubnetMetas() == null || source.getGatewayEndpointSubnetMetas().isEmpty()) {
                subnetsToParse = source.getSubnetMetas();
            } else {
                subnetsToParse = source.getGatewayEndpointSubnetMetas();
            }
            Map<String, CloudSubnet> publicSubnetMetas = subnetsToParse.entrySet().stream()
                .filter(entry -> !entry.getValue().isPrivateSubnet())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Optional<CloudSubnet> endpointGatewayCloudSubnet = chooseSubnet(null,
                publicSubnetMetas, selectedAZ, false);
            if (endpointGatewayCloudSubnet.isEmpty()) {
                throw new BadRequestException("Could not find public subnet in availability zone: " + selectedAZ);
            }
            LOGGER.debug("Chosen endpoint gateway subnet: {}", endpointGatewayCloudSubnet.get());
            attributes.put("endpointGatewaySubnetId", endpointGatewayCloudSubnet.get().getId());
        }
    }

    abstract Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source);
}
