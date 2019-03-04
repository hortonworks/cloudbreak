package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class UpscaleStackValidationResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    public UpscaleStackValidationResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public UpscaleStackValidationResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }
}
