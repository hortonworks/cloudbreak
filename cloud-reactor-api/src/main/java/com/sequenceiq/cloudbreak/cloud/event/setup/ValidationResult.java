package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class ValidationResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    public ValidationResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public ValidationResult(Exception errorDetails, CloudPlatformRequest<?> request) {
        this(errorDetails.getMessage(), errorDetails, request);
    }

    public ValidationResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

}
