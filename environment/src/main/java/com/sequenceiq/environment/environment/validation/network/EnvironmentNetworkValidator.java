package com.sequenceiq.environment.environment.validation.network;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentNetworkValidator {

    void validateDuringFlow(EnvironmentDto environmentDto, NetworkDto networkDto, ValidationResultBuilder resultBuilder);

    void validateDuringRequest(NetworkDto networkDto, ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String missingParamErrorMessage(String paramName, String cloudPlatform) {
        return String.format("The '%s' parameter should be specified for the '%s' environment specific network!", paramName, cloudPlatform);
    }

    default String missingParamsErrorMsg(CloudPlatform cloudPlatform) {
        return String.format("The '%s' related network parameters should be specified!", cloudPlatform);
    }

    default Set<String> getSubnetDiff(Set<String> subnets, Set<String> subnetMetaKeys) {
        Set<String> diff = new HashSet<>();
        for (String envSubnet : subnets) {
            if (!subnetMetaKeys.contains(envSubnet)) {
                diff.add(envSubnet);
            }
        }
        return diff;
    }

}
