package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class CheckPlatformVariantResult extends CloudPlatformResult {
    private Variant defaultPlatformVariant;

    public CheckPlatformVariantResult(Long resourceId, Variant defaultPlatformVariant) {
        super(resourceId);
        this.defaultPlatformVariant = defaultPlatformVariant;
    }

    public CheckPlatformVariantResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Variant getDefaultPlatformVariant() {
        return defaultPlatformVariant;
    }
}
