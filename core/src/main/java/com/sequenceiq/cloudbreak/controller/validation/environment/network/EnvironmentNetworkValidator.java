package com.sequenceiq.cloudbreak.controller.validation.environment.network;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;

public interface EnvironmentNetworkValidator {

    void validate(EnvironmentNetworkV4Request networkV4Request, ValidationResult.ValidationResultBuilder resultBuilder);

    CloudPlatform getCloudPlatform();

    default String getCloudPlatformName() {
        return getCloudPlatform().name();
    }

    default String missingParamErrorMessage(String paramName) {
        return String.format("The '%s' parameter should be specified for the '%s' environment specific network!", paramName, getCloudPlatformName());
    }

    default String missingParamsErrorMsg() {
        return String.format("The '%s' related network parameters should be specified!", getCloudPlatformName());
    }
}
