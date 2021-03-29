package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class SubnetSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelector.class);

    public Optional<CloudSubnet> chooseSubnet(String preferredSubnetId, Map<String, CloudSubnet> subnetMetas, String availabilityZone, boolean allowRandom) {
        Optional<CloudSubnet> cloudSubnet;
        if (StringUtils.isNotEmpty(preferredSubnetId)) {
            cloudSubnet = findSubnetById(subnetMetas, preferredSubnetId);
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

    public Optional<CloudSubnet> chooseSubnetForEndpointGateway(EnvironmentNetworkResponse source, String baseSubnetId) {
        Optional<CloudSubnet> endpointGatewayCloudSubnet = Optional.empty();
        if (source.getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED) {
            Optional<CloudSubnet> baseSubnet = findSubnetById(source.getSubnetMetas(), baseSubnetId);
            if (baseSubnet.isEmpty()) {
                LOGGER.error("Unable to find subnet with id {}", baseSubnet);
            } else {
                String selectedAZ = baseSubnet.get().getAvailabilityZone();
                Map<String, CloudSubnet> subnetsToParse;
                if (source.getGatewayEndpointSubnetMetas() == null || source.getGatewayEndpointSubnetMetas().isEmpty()) {
                    subnetsToParse = source.getSubnetMetas();
                } else {
                    subnetsToParse = source.getGatewayEndpointSubnetMetas();
                }
                Map<String, CloudSubnet> publicSubnetMetas = subnetsToParse.entrySet().stream()
                    .filter(entry -> !entry.getValue().isPrivateSubnet() || entry.getValue().isRoutableToInternet())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                endpointGatewayCloudSubnet = chooseSubnet(null, publicSubnetMetas, selectedAZ, false);
                if (endpointGatewayCloudSubnet.isPresent()) {
                    LOGGER.debug("Chosen endpoint gateway subnet: {}", endpointGatewayCloudSubnet.get());
                } else {
                    LOGGER.debug("Could not find public subnet in availability zone {}", selectedAZ);
                }
            }
        }
        return endpointGatewayCloudSubnet;
    }

    public Optional<CloudSubnet> findSubnetById(Map<String, CloudSubnet> subnetMetas, String id) {
        CloudSubnet cloudSubnetById = subnetMetas.get(id);
        if (cloudSubnetById == null) {
            cloudSubnetById = subnetMetas.values()
                .stream()
                .filter(e -> e.getId().equals(id))
                .findFirst().orElse(null);
        }
        return Optional.ofNullable(cloudSubnetById);
    }
}
