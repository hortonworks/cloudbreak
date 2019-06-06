package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class ParametersValidationResult extends CloudPlatformResult {

    public ParametersValidationResult(Long resourceId) {
        super(resourceId);
    }

    public ParametersValidationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
