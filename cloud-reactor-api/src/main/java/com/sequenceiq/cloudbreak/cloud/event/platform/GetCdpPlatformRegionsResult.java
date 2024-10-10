package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;

public class GetCdpPlatformRegionsResult extends CloudPlatformResult {
    private CloudRegions cloudRegions;

    public GetCdpPlatformRegionsResult(CloudRegions cloudRegions) {
        super(null);
        this.cloudRegions = cloudRegions;
    }

    public GetCdpPlatformRegionsResult(String statusReason, Exception errorDetails) {
        super(statusReason, errorDetails, null);
    }

    public GetCdpPlatformRegionsResult(EventStatus status, String statusReason, Exception errorDetails) {
        super(status, statusReason, errorDetails, null);
    }

    public CloudRegions getCloudRegions() {
        return cloudRegions;
    }

    @Override
    public String toString() {
        return "GetCdpPlatformRegionsResult{" +
                "cloudRegions=" + cloudRegions +
                '}';
    }
}
