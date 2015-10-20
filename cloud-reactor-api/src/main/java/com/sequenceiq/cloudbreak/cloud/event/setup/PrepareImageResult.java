package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class PrepareImageResult extends CloudPlatformResult {

    public PrepareImageResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public PrepareImageResult(Exception errorDetails, CloudPlatformRequest<?> request) {
        this(errorDetails.getMessage(), errorDetails, request);
    }

    public PrepareImageResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

}
