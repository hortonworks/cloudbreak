package com.sequenceiq.environment.environment.validation.network.aws;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    public AwsEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService) {
        this.cloudNetworkService = cloudNetworkService;
    }

    @Override
    public void validateDuringFlow(EnvironmentValidationDto environmentValidationDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto == null || networkDto.getRegistrationType() != RegistrationType.EXISTING) {
            return;
        }
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        if (!validateMainNetworkSubnets(resultBuilder, environmentDto, networkDto)) {
            return;
        }
        if (CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds())) {
            validateEndpointGatewaySubnets(resultBuilder, environmentDto, networkDto);
        }
    }

    private boolean validateMainNetworkSubnets(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto, NetworkDto networkDto) {
        if (isMissingCidrAndNetwork(networkDto)) {
            logAndAddError(resultBuilder, "Either the AWS network ID or CIDR needs to be defined!");
            return false;
        }
        Map<String, CloudSubnet> cloudSubnetMetadata = cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto);
        if (hasUnresolvedSubnets(networkDto.getSubnetMetas(), cloudSubnetMetadata)) {
            addSubnetResolutionErrors(resultBuilder, "Subnet IDs", environmentDto, networkDto, networkDto.getSubnetMetas(), cloudSubnetMetadata);
            return false;
        }
        if (hasTooFewSubnets(cloudSubnetMetadata)) {
            logAndAddError(resultBuilder, "There should be at least two subnets in the environment network configuration.");
            return false;
        }
        Set<String> subnetAvailabilityZones = subnetAvailabilityZones(cloudSubnetMetadata);
        if (hasTooFewAvailabilityZones(subnetAvailabilityZones)) {
            String subnetNames = cloudSubnetMetadata.values()
                    .stream()
                    .map(CloudSubnet::getName)
                    .collect(Collectors.joining(", "));
            String availabilityZones = String.join(", ", subnetAvailabilityZones);
            logAndAddError(resultBuilder, String.format("The subnets (%s) should be present in at least two different " +
                            "availability zones, but they are present only in %s. "
                            + "Please add subnets from at least two different availability zones.",
                    subnetNames, availabilityZones));
            return false;
        }
        return true;
    }

    private void validateEndpointGatewaySubnets(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto, NetworkDto networkDto) {
        Map<String, CloudSubnet> cloudLoadBalancerSubnetMetadata =
                cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto);
        if (hasUnresolvedSubnets(networkDto.getEndpointGatewaySubnetMetas(), cloudLoadBalancerSubnetMetadata)) {
            addSubnetResolutionErrors(resultBuilder, "Endpoint gateway subnet IDs", environmentDto, networkDto,
                    networkDto.getEndpointGatewaySubnetMetas(), cloudLoadBalancerSubnetMetadata);
        }
        Map<String, List<CloudSubnet>> zonesWithMultipleCloudSubnets = zonesWithMultipleSubnets(cloudLoadBalancerSubnetMetadata);
        if (!zonesWithMultipleCloudSubnets.isEmpty()) {
            String subnetsByZone = zonesWithMultipleCloudSubnets.entrySet().stream()
                    .map(zoneWithSubnets -> String.format("%s (subnets: %s)",
                            zoneWithSubnets.getKey(),
                            zoneWithSubnets.getValue().stream().map(CloudSubnet::getId).collect(Collectors.joining(", "))))
                    .collect(Collectors.joining("; "));
            logAndAddError(resultBuilder, String.format("Environment '%s' has been requested with an invalid public endpoint access gateway setup. "
                            + "Select only one endpoint gateway subnet per availability zone. "
                            + "The following availability zones have multiple selected subnets: %s.",
                    environmentDto.getName(), subnetsByZone));
        }
    }

    private boolean isMissingCidrAndNetwork(NetworkDto networkDto) {
        return StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId());
    }

    private boolean hasUnresolvedSubnets(Map<String, CloudSubnet> subnetMetas, Map<String, CloudSubnet> subnetsFromProvider) {
        return subnetMetas.size() != subnetsFromProvider.size();
    }

    private void addSubnetResolutionErrors(ValidationResultBuilder resultBuilder, String context, EnvironmentDto environmentDto, NetworkDto networkDto,
            Map<String, CloudSubnet> subnetMetas, Map<String, CloudSubnet> subnetsFromProvider) {
        Set<String> missingSubnetIds = new HashSet<>(SetUtils.difference(subnetMetas.keySet(), subnetsFromProvider.keySet()));
        Map<String, String> unsupportedAvailabilityZoneSubnets =
                cloudNetworkService.findAwsSubnetsInUnsupportedAvailabilityZones(environmentDto, networkDto, missingSubnetIds);
        missingSubnetIds.removeAll(unsupportedAvailabilityZoneSubnets.keySet());

        if (!unsupportedAvailabilityZoneSubnets.isEmpty()) {
            String subnetsWithZones = unsupportedAvailabilityZoneSubnets.entrySet().stream()
                    .map(entry -> String.format("%s in %s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(", "));
            logAndAddError(resultBuilder, String.format("%s (%s) of the environment (%s) are in availability zones not supported by the platform. "
                            + "Please select subnets from supported availability zones.",
                    context, subnetsWithZones, environmentDto.getName()));
        }
        if (!missingSubnetIds.isEmpty()) {
            logAndAddError(resultBuilder, String.format("%s (%s) of the environment (%s) are not found in the VPC. "
                            + "All subnets are expected to belong to the same VPC.",
                    context, String.join(", ", missingSubnetIds), environmentDto.getName()));
        }
    }

    private boolean hasTooFewSubnets(Map<String, CloudSubnet> cloudSubnetMetadata) {
        return cloudSubnetMetadata.size() < 2;
    }

    private boolean hasTooFewAvailabilityZones(Set<String> availabilityZones) {
        return availabilityZones.size() < 2;
    }

    private Map<String, List<CloudSubnet>> zonesWithMultipleSubnets(Map<String, CloudSubnet> cloudLoadBalancerSubnetMetadata) {
        return cloudLoadBalancerSubnetMetadata.values()
                .stream()
                .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone))
                .entrySet()
                .stream()
                .filter(zoneWithSubnets -> zoneWithSubnets.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> subnetAvailabilityZones(Map<String, CloudSubnet> cloudSubnetMetadata) {
        return cloudSubnetMetadata.values().stream()
                .map(CloudSubnet::getAvailabilityZone)
                .collect(Collectors.toSet());
    }

    private void logAndAddError(ValidationResultBuilder resultBuilder, String message) {
        LOGGER.info(message);
        resultBuilder.error(message);
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto != null && isNetworkExisting(networkDto)) {
            LOGGER.debug("Validation - existing - AWS network param(s) during request time");
            if (networkDto.getAws() != null) {
                if (StringUtils.isEmpty(networkDto.getAws().getVpcId())) {
                    resultBuilder.error(missingParamErrorMessage("VPC identifier(vpcId)", getCloudPlatform().name()));
                }
            } else {
                resultBuilder.error(missingParamsErrorMsg(AWS));
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

}
