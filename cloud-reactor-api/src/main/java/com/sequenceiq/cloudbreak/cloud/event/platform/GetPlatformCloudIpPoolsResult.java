package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;

public class GetPlatformCloudIpPoolsResult extends CloudPlatformResult {
    private CloudIpPools cloudIpPools;

    public GetPlatformCloudIpPoolsResult(Long resourceId, CloudIpPools cloudIpPools) {
        super(resourceId);
        this.cloudIpPools = cloudIpPools;
    }

    public GetPlatformCloudIpPoolsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudIpPools getCloudIpPools() {
        return cloudIpPools;
    }
}
