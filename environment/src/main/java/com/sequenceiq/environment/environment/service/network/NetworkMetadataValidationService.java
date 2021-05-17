package com.sequenceiq.environment.environment.service.network;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;

@Component
public class NetworkMetadataValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkMetadataValidationService.class);

    private static final String UNMATCHED_AZ = "Please provide public subnets in each of the following availability zones: %s." +
            "You need to define subnets which are in different availability zones because CDP would like to provision highly available resources.";

    private final CloudNetworkService cloudNetworkService;

    private final EntitlementService entitlementService;

    public NetworkMetadataValidationService(CloudNetworkService cloudNetworkService,
            EntitlementService entitlementService) {
        this.cloudNetworkService = cloudNetworkService;
        this.entitlementService = entitlementService;
    }

    public Map<String, CloudSubnet> getEndpointGatewaySubnetMetadata(Environment environment, EnvironmentDto environmentDto) {
        Map<String, CloudSubnet> endpointGatewaySubnetMetas = null;
        LOGGER.debug("Fetching subnet metadata for from cloud providers.");
        endpointGatewaySubnetMetas = removePrivateSubnets(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(
            environmentDto, environmentDto.getNetwork()));
        validateSubnetsIfProvided(environment, environmentDto.getNetwork().getSubnetMetas(), endpointGatewaySubnetMetas);
        return endpointGatewaySubnetMetas;
    }

    private Map<String, CloudSubnet> removePrivateSubnets(Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        if (entitlementService.endpointGatewaySkipValidation(ThreadBasedUserCrnProvider.getAccountId())) {
            LOGGER.debug("Endpoint gateway subnet type validation is disabled. Accepting provided subnets");
            return endpointGatewaySubnetMetas;
        } else {
            LOGGER.debug("Removing any private subnets from the provided endpoint gateway list because they won't be used.");
            if (endpointGatewaySubnetMetas == null || endpointGatewaySubnetMetas.isEmpty()) {
                return Map.of();
            }
            return endpointGatewaySubnetMetas.entrySet().stream()
                .filter(entry -> !entry.getValue().isPrivateSubnet())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private void validateSubnetsIfProvided(Environment environment, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        if (shouldValidateEndpointGatewaySubnets(environment, subnetMetas)) {
            if (endpointGatewaySubnetMetas != null && !endpointGatewaySubnetMetas.isEmpty()) {
                LOGGER.info("Validation of endpoint gateway subnets failed: gateway subnets should share availability " +
                        "zones with the provided environment subnets.");
                valildateProvidedEndpointGatewaySubnets(subnetMetas, endpointGatewaySubnetMetas);
            } else {
                LOGGER.info("Validation of endpoint gateway subnets failed: Please make sure to provide a public " +
                        "subnet for every availability zone where the environment has a private subnet.");
                valildateEnvironmentSubnetsForEndpointGateway(subnetMetas);
            }
        }
    }

    private boolean shouldValidateEndpointGatewaySubnets(Environment environment, Map<String, CloudSubnet> subnetMetas) {
        return hasSupportedNetwork(environment) &&
            environment.getNetwork().getPublicEndpointAccessGateway() == PublicEndpointAccessGateway.ENABLED &&
            environment.getNetwork().getRegistrationType() != RegistrationType.CREATE_NEW &&
            subnetMetas != null &&
            !subnetMetas.isEmpty();
    }

    private void valildateProvidedEndpointGatewaySubnets(Map<String, CloudSubnet> subnetMetas, Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        LOGGER.debug("Verifying that provided endpoint gateway subnets share availability zones with provided environment subnets.");
        Set<String> subnetAZs = subnetMetas.values().stream()
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        Set<String> endpointGatewaySubnetAZs = endpointGatewaySubnetMetas.values().stream()
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        if (!endpointGatewaySubnetAZs.containsAll(subnetAZs)) {
            throw new BadRequestException(String.format(UNMATCHED_AZ, subnetAZs));
        }
    }

    private void valildateEnvironmentSubnetsForEndpointGateway(Map<String, CloudSubnet> subnetMetas) {
        LOGGER.debug("Verifying that public subnets in availability zones that match the private subnets were provided.");
        Set<String> privateAZs = subnetMetas.values().stream()
            .filter(CloudSubnet::isPrivateSubnet)
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        Set<String> publicAZs = subnetMetas.values().stream()
            .filter(subnet -> !subnet.isPrivateSubnet())
            .map(CloudSubnet::getAvailabilityZone)
            .collect(Collectors.toSet());
        if (!publicAZs.containsAll(privateAZs)) {
            throw new BadRequestException(String.format(UNMATCHED_AZ, privateAZs));
        }
    }

    private boolean hasSupportedNetwork(Environment environment) {
        return Objects.nonNull(environment.getNetwork())
            && AWS.equalsIgnoreCase(environment.getCloudPlatform());
    }
}
