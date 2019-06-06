package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class UpdateImageResult extends CloudPlatformResult {

    public UpdateImageResult(Long resourceId) {
        super(resourceId);
    }

    public UpdateImageResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
