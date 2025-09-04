package com.sequenceiq.environment.environment.validation.network.aws;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();

        if (networkDto != null && networkDto.getRegistrationType() == RegistrationType.EXISTING) {
            if (missingCidrOrNetwork(resultBuilder, networkDto)) {
                return;
            }
            Map<String, CloudSubnet> cloudSubnetMetadata = cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto);
            if (subnetsNotFoundInVpc(resultBuilder, "Subnet IDs", environmentDto, networkDto.getSubnetMetas(), cloudSubnetMetadata)) {
                return;
            }
            if (tooFewSubnets(resultBuilder, cloudSubnetMetadata)) {
                return;
            }
            if (tooFewAvailabilityZones(resultBuilder, cloudSubnetMetadata)) {
                return;
            }
            if (CollectionUtils.isNotEmpty(networkDto.getEndpointGatewaySubnetIds())) {
                Map<String, CloudSubnet> cloudLoadBalancerSubnetMetadata =
                        cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environmentDto, networkDto);
                subnetsNotFoundInVpc(resultBuilder, "Endpoint gateway subnet IDs", environmentDto, networkDto.getEndpointGatewaySubnetMetas(),
                        cloudLoadBalancerSubnetMetadata);
                subnetsHaveDifferentAvailabilityZones(resultBuilder, environmentDto, cloudLoadBalancerSubnetMetadata);
            }
        }
    }

    private boolean missingCidrOrNetwork(ValidationResultBuilder resultBuilder, NetworkDto networkDto) {
        if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
            String message = "Either the AWS network ID or CIDR needs to be defined!";
            LOGGER.info(message);
            resultBuilder.error(message);
            return true;
        }
        return false;
    }

    private boolean subnetsNotFoundInVpc(ValidationResultBuilder resultBuilder, String context, EnvironmentDto environmentDto,
            Map<String, CloudSubnet> subnetMetas, Map<String, CloudSubnet> subnetsFromProvider) {
        if (subnetMetas.size() != subnetsFromProvider.size()) {
            String message = String.format("%s of the environment (%s) are not found in the VPC (%s). All subnets are expected to belong to the same VPC.",
                    context, environmentDto.getName(), String.join(", ", SetUtils.difference(subnetMetas.keySet(), subnetsFromProvider.keySet())));
            LOGGER.info(message);
            resultBuilder.error(message);
            return true;
        }
        return false;
    }

    private boolean tooFewSubnets(ValidationResultBuilder resultBuilder, Map<String, CloudSubnet> cloudmetadata) {
        if (cloudmetadata.size() < 2) {
            String message = "There should be at least two Subnets in the environment network configuration.";
            LOGGER.info(message);
            resultBuilder.error(message);
            return true;
        }
        return false;
    }

    private boolean tooFewAvailabilityZones(ValidationResultBuilder resultBuilder, Map<String, CloudSubnet> cloudmetadata) {
        Map<String, Long> zones = cloudmetadata.values().stream()
                .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
        if (zones.size() < 2) {
            String message = String.format("The Subnets in the VPC (%s) should be present at least in two different " +
                            "availability zones, but they are present only in availability zone %s. Please add " +
                            "subnets to the environment from the required number of different availability zones.",
                    String.join(", ", new ArrayList<>(zones.keySet())),
                    cloudmetadata.values()
                            .stream()
                            .map(CloudSubnet::getName)
                            .collect(Collectors.joining(", ")));
            LOGGER.info(message);
            resultBuilder.error(message);
            return true;
        }
        return false;
    }

    private void subnetsHaveDifferentAvailabilityZones(ValidationResultBuilder resultBuilder, EnvironmentDto environmentDto,
            Map<String, CloudSubnet> cloudLoadBalancerSubnetMetadata) {
        Map<String, List<CloudSubnet>> zonesWithMultipleCloudSubnets = cloudLoadBalancerSubnetMetadata.values()
                .stream()
                .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone))
                .entrySet()
                .stream()
                .filter(cloudSubnetListByAvailabilityZone -> cloudSubnetListByAvailabilityZone.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!zonesWithMultipleCloudSubnets.isEmpty()) {
            String errorWithSubnetsByZones = zonesWithMultipleCloudSubnets.entrySet().stream()
                    .map(subnetListByZone -> String.format("subnets '%s' are from zone '%s'",
                            subnetListByZone.getValue().stream().map(CloudSubnet::getId).collect(Collectors.joining(", ")),
                            subnetListByZone.getKey()))
                    .collect(Collectors.joining(", "));
            String message = String.format("Environment '%s' has been requested with invalid public endpoint access gateway setup. "
                            + "The selected subnets must have different Availability Zones, which means select one subnet per zone only. But %s.",
                    environmentDto.getName(), errorWithSubnetsByZones);
            LOGGER.info(message);
            resultBuilder.error(message);
        }
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
