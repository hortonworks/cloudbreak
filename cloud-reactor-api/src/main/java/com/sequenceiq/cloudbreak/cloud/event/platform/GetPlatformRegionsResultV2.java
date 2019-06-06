package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;

public class GetPlatformRegionsResultV2 extends CloudPlatformResult {
    private CloudRegions cloudRegions;

    public GetPlatformRegionsResultV2(Long resourceId, CloudRegions cloudRegions) {
        super(resourceId);
        this.cloudRegions = cloudRegions;
    }

    public GetPlatformRegionsResultV2(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudRegions getCloudRegions() {
        return cloudRegions;
    }
}
