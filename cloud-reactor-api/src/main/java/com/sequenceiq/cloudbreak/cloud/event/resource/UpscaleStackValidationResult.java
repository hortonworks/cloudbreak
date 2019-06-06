package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class UpscaleStackValidationResult extends CloudPlatformResult {
    public UpscaleStackValidationResult(Long resourceId) {
        super(resourceId);
    }

    public UpscaleStackValidationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
