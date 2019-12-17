package com.sequenceiq.environment.environment.validation.network;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.NetworkDto;

public interface EnvironmentNetworkValidator {

    void validateDuringFlow(NetworkDto networkV1Request, ValidationResult.ValidationResultBuilder resultBuilder);

    void validateDuringRequest(NetworkDto networkV1Request, Map<String, CloudSubnet> subnetMetas, ValidationResult.ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String missingParamErrorMessage(String paramName, String cloudPlatform) {
        return String.format("The '%s' parameter should be specified for the '%s' environment specific network!", paramName, cloudPlatform);
    }

    default String missingParamsErrorMsg(CloudPlatform cloudPlatform) {
        return String.format("The '%s' related network parameters should be specified!", cloudPlatform);
    }
}
