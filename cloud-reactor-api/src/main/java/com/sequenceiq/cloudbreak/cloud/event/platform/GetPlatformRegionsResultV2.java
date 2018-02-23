package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;

public class GetPlatformRegionsResultV2 extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private CloudRegions cloudRegions;

    public GetPlatformRegionsResultV2(CloudPlatformRequest<?> request, CloudRegions cloudRegions) {
        super(request);
        this.cloudRegions = cloudRegions;
    }

    public GetPlatformRegionsResultV2(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudRegions getCloudRegions() {
        return cloudRegions;
    }
}
