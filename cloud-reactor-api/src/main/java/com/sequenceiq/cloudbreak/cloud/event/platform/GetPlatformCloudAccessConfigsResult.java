package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;

public class GetPlatformCloudAccessConfigsResult extends CloudPlatformResult {
    private CloudAccessConfigs cloudAccessConfigs;

    public GetPlatformCloudAccessConfigsResult(Long resourceId, CloudAccessConfigs cloudAccessConfigs) {
        super(resourceId);
        this.cloudAccessConfigs = cloudAccessConfigs;
    }

    public GetPlatformCloudAccessConfigsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public GetPlatformCloudAccessConfigsResult(EventStatus status, String statusReason, Exception errorDetails, Long resourceId) {
        super(status, statusReason, errorDetails, resourceId);
    }

    public CloudAccessConfigs getCloudAccessConfigs() {
        return cloudAccessConfigs;
    }
}