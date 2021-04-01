package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class SubnetSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelector.class);

    @Inject
    private EntitlementService entitlementService;

    public Optional<CloudSubnet> chooseSubnet(String preferredSubnetId, Map<String, CloudSubnet> subnetMetas, String availabilityZone,
            SelectionFallbackStrategy fallbackStrategy) {
        return chooseSubnet(preferredSubnetId, subnetMetas, availabilityZone, fallbackStrategy, SelectionVisibilityStrategy.DEFAULT);
    }

    @VisibleForTesting
    Optional<CloudSubnet> chooseSubnetPreferPublic(String preferredSubnetId, Map<String, CloudSubnet> subnetMetas, String availabilityZone) {
        return chooseSubnet(preferredSubnetId, subnetMetas, availabilityZone, SelectionFallbackStrategy.NO_FALLBACK,
            SelectionVisibilityStrategy.PREFER_PUBLIC);
    }

    private Optional<CloudSubnet> chooseSubnet(String preferredSubnetId, Map<String, CloudSubnet> subnetMetas, String availabilityZone,
            SelectionFallbackStrategy fallbackStrategy, SelectionVisibilityStrategy visibilityStrategy) {
        Optional<CloudSubnet> cloudSubnet = Optional.empty();
        if (StringUtils.isNotEmpty(preferredSubnetId)) {
            cloudSubnet = findSubnetById(subnetMetas, preferredSubnetId);
        } else if (StringUtils.isNotEmpty(availabilityZone)) {
            LOGGER.debug("Choosing subnet by availability zone {}", availabilityZone);
            if (SelectionVisibilityStrategy.PREFER_PUBLIC.equals(visibilityStrategy)) {
                LOGGER.debug("Attempting to select a public subnet in availability zone {} from the provided list.", availabilityZone);
                cloudSubnet = subnetMetas.values().stream()
                    .filter(s -> StringUtils.isNotEmpty(s.getAvailabilityZone()) &&
                        s.getAvailabilityZone().equals(availabilityZone) &&
                        !s.isPrivateSubnet())
                    .findFirst();
                if (cloudSubnet.isEmpty()) {
                    LOGGER.debug("Public subnet in availability zone {} was not found.", availabilityZone);
                }
            }
            if (cloudSubnet.isEmpty()) {
                LOGGER.debug("Searching for subnet in availabilty zone {}; no preference for public vs. private subnet", availabilityZone);
                cloudSubnet = subnetMetas.values().stream()
                    .filter(s -> StringUtils.isNotEmpty(s.getAvailabilityZone()) &&
                        s.getAvailabilityZone().equals(availabilityZone))
                    .findFirst();
            }
        } else if (SelectionFallbackStrategy.ALLOW_FALLBACK.equals(fallbackStrategy)) {
            LOGGER.debug("Fallback to choose random subnet");
            cloudSubnet = subnetMetas.values().stream().findFirst();
        }

        cloudSubnet.ifPresentOrElse(
            s -> LOGGER.info("Selected subnet: {}", s.getId()),
            () -> LOGGER.info("Unable to identify subnet with requested parameters [AZ: {}, {}, {}]. No subnet was selected.",
                availabilityZone, visibilityStrategy, fallbackStrategy));
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

                Map<String, CloudSubnet> publicSubnetMetas;
                if (entitlementService.endpointGatewaySkipValidation(ThreadBasedUserCrnProvider.getAccountId())) {
                    LOGGER.debug("Endpoint gateway subnet type validation is disabled. Will use all provided subnets for selection.");
                    publicSubnetMetas = subnetsToParse;
                } else {
                    LOGGER.debug("Searching endpoint gateway subnets for public subnets.");
                    publicSubnetMetas = subnetsToParse.entrySet().stream()
                        .filter(entry -> !entry.getValue().isPrivateSubnet())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }

                endpointGatewayCloudSubnet = chooseSubnetPreferPublic(null, publicSubnetMetas, selectedAZ);
                if (endpointGatewayCloudSubnet.isPresent()) {
                    LOGGER.debug("Chosen endpoint gateway subnet: {}", endpointGatewayCloudSubnet.get());
                } else {
                    LOGGER.debug("Could not find valid subnet in availability zone {}", selectedAZ);
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
