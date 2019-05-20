package com.sequenceiq.environment.environment.validator.network;


import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;

public interface EnvironmentNetworkValidator {

    void validate(EnvironmentNetworkV1Request networkV1Request, ValidationResult.ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String missingParamErrorMessage(String paramName, String cloudPlatform) {
        return String.format("The '%s' parameter should be specified for the '%s' environment specific network!", paramName, cloudPlatform);
    }

    default String missingParamsErrorMsg(CloudPlatform cloudPlatform) {
        return String.format("The '%s' related network parameters should be specified!", cloudPlatform);
    }
}
