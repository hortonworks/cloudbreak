package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;

public class GetPlatformVariantsResult extends CloudPlatformResult {
    private PlatformVariants platformVariants;

    public GetPlatformVariantsResult(CloudPlatformRequest<?> request, PlatformVariants platformVariants) {
        super(request);
        this.platformVariants = platformVariants;
    }

    public GetPlatformVariantsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformVariants getPlatformVariants() {
        return platformVariants;
    }
}
