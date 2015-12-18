package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;

public class GetPlatformRegionsResult extends CloudPlatformResult<CloudPlatformRequest> {
    private PlatformRegions platformRegions;

    public GetPlatformRegionsResult(CloudPlatformRequest<?> request, PlatformRegions platformRegions) {
        super(request);
        this.platformRegions = platformRegions;
    }

    public GetPlatformRegionsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformRegions getPlatformRegions() {
        return platformRegions;
    }
}
