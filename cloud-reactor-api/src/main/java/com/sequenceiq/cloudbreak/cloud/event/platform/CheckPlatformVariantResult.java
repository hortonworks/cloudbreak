package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class CheckPlatformVariantResult extends CloudPlatformResult {
    private Variant defaultPlatformVariant;

    public CheckPlatformVariantResult(CloudPlatformRequest<?> request, Variant defaultPlatformVariant) {
        super(request);
        this.defaultPlatformVariant = defaultPlatformVariant;
    }

    public CheckPlatformVariantResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public Variant getDefaultPlatformVariant() {
        return defaultPlatformVariant;
    }
}
