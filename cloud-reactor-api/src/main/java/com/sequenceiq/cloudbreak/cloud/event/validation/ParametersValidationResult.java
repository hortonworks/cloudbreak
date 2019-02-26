package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class ParametersValidationResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    public ParametersValidationResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public ParametersValidationResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }
}
