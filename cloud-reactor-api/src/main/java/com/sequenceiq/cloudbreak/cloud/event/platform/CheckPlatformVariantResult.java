package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class CheckPlatformVariantResult extends CloudPlatformResult {
    private String defaultPlatformVariant;

    public CheckPlatformVariantResult(CloudPlatformRequest<?> request, String defaultPlatformVariant) {
        super(request);
        this.defaultPlatformVariant = defaultPlatformVariant;
    }

    public CheckPlatformVariantResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public String getDefaultPlatformVariant() {
        return defaultPlatformVariant;
    }
}
