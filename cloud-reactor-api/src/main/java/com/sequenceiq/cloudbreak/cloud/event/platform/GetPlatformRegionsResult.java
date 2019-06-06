package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;

public class GetPlatformRegionsResult extends CloudPlatformResult {
    private PlatformRegions platformRegions;

    public GetPlatformRegionsResult(Long resourceId, PlatformRegions platformRegions) {
        super(resourceId);
        this.platformRegions = platformRegions;
    }

    public GetPlatformRegionsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public PlatformRegions getPlatformRegions() {
        return platformRegions;
    }
}
