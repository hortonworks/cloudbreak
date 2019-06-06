package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class PrepareImageResult extends CloudPlatformResult {

    public PrepareImageResult(Long resourceId) {
        super(resourceId);
    }

    public PrepareImageResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    public PrepareImageResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

}
