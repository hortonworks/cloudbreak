package com.sequenceiq.environment.environment.validation.network;


import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkValidator implements EnvironmentNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentNetworkValidator.class);

    @Override
    public void validateDuringFlow(NetworkDto networkDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        if (networkDto != null) {
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
    public void validateDuringRequest(
            NetworkDto networkV1Request, Map<String, CloudSubnet> subnetMetas, ValidationResult.ValidationResultBuilder resultBuilder) {
        String message;
        if (networkV1Request.getSubnetIds().size() < 2) {
            message = "Cannot create environment, there should be at least two subnets in the network";
            LOGGER.info(message);
            resultBuilder.error(message);
        }
        if (subnetMetas.size() != networkV1Request.getSubnetIds().size()) {
            message = String.format("Subnets of the environment (%s) are not found in the vpc (%s). ",
                    String.join(",", getSubnetDiff(networkV1Request.getSubnetIds(), subnetMetas.keySet())),
                    Optional.of(networkV1Request).map(NetworkDto::getAws).map(AwsParams::getVpcId).orElse(""));
            LOGGER.info(message);
            resultBuilder.error(message);
        }
        Map<String, Long> zones = subnetMetas.values().stream()
                .collect(Collectors.groupingBy(CloudSubnet::getAvailabilityZone, Collectors.counting()));
        if (zones.size() < 2) {
            message = "Cannot create environment, the subnets in the vpc should be present at least in two different availability zones";
            LOGGER.info(message);
            resultBuilder.error(message);
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

    private Set<String> getSubnetDiff(Set<String> envSubnets, Set<String> vpcSubnets) {
        Set<String> diff = new HashSet<>();
        for (String envSubnet : envSubnets) {
            if (!vpcSubnets.contains(envSubnet)) {
                diff.add(envSubnet);
            }
        }
        return diff;
    }
}
