package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;

public class GetDiskTypesResult extends CloudPlatformResult {
    private PlatformDisks platformDisks;

    public GetDiskTypesResult(Long resourceId, PlatformDisks platformDisks) {
        super(resourceId);
        this.platformDisks = platformDisks;
    }

    public GetDiskTypesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public PlatformDisks getPlatformDisks() {
        return platformDisks;
    }
}
