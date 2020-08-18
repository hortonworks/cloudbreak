package com.sequenceiq.environment.environment.validation.network.aws;


import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.environment.validation.network.EnvironmentNetworkValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentNetworkValidator.class);

    private final CloudNetworkService cloudNetworkService;

    public AwsEnvironmentNetworkValidator(CloudNetworkService cloudNetworkService) {
        this.cloudNetworkService = cloudNetworkService;
    }

    @Override
    public void validateDuringFlow(EnvironmentDto environmentDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        String message;
        if (networkDto != null && networkDto.getRegistrationType() == RegistrationType.EXISTING) {
            Map<String, CloudSubnet> cloudmetadata = cloudNetworkService.retrieveSubnetMetadata(environmentDto, networkDto);
            if (StringUtils.isEmpty(networkDto.getNetworkCidr()) && StringUtils.isEmpty(networkDto.getNetworkId())) {
                message = "Either the AWS network id or cidr needs to be defined!";
                LOGGER.info(message);
                resultBuilder.error(message);
                return;
            }
            if (networkDto.getSubnetMetas().size() != cloudmetadata.size()) {
                message = String.format("Subnets of the environment (%s) are not found in the vpc (%s).",
                        environmentDto.getName(), String.join(", ", getSubnetDiff(networkDto.getSubnetIds(), cloudmetadata.keySet())));
                LOGGER.info(message);
                resultBuilder.error(message);
                return;
            }
            if (cloudmetadata.size() < 2) {
                message = "There should be at least two subnets in the network";
                LOGGER.info(message);
                resultBuilder.error(message);
                return;
            }
            Map<String, Long> zones = cloudmetadata.values().stream()
                    .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
            if (zones.size() < 2) {
                message = "The subnets in the vpc should be present at least in two different availability zones";
                LOGGER.info(message);
                resultBuilder.error(message);
            }
        }
    }

    @Override
    public void validateDuringRequest(NetworkDto networkDto, ValidationResultBuilder resultBuilder) {
        if (networkDto != null && isNetworkExisting(networkDto)) {
            LOGGER.debug("Validation - existing - AWS network param(s) during requiest time");
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
