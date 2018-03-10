package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;

public class GetPlatformCloudIpPoolsResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private CloudIpPools cloudIpPools;

    public GetPlatformCloudIpPoolsResult(CloudPlatformRequest<?> request, CloudIpPools cloudIpPools) {
        super(request);
        this.cloudIpPools = cloudIpPools;
    }

    public GetPlatformCloudIpPoolsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudIpPools getCloudIpPools() {
        return cloudIpPools;
    }
}
