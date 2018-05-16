package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;

public class GetPlatformCloudAccessConfigsResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private CloudAccessConfigs cloudAccessConfigs;

    public GetPlatformCloudAccessConfigsResult(CloudPlatformRequest<?> request, CloudAccessConfigs cloudAccessConfigs) {
        super(request);
        this.cloudAccessConfigs = cloudAccessConfigs;
    }

    public GetPlatformCloudAccessConfigsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudAccessConfigs getCloudAccessConfigs() {
        return cloudAccessConfigs;
    }
}