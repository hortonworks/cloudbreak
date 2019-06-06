package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;

public class GetPlatformVariantsResult extends CloudPlatformResult {
    private PlatformVariants platformVariants;

    public GetPlatformVariantsResult(Long resourceId, PlatformVariants platformVariants) {
        super(resourceId);
        this.platformVariants = platformVariants;
    }

    public GetPlatformVariantsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public PlatformVariants getPlatformVariants() {
        return platformVariants;
    }
}
