package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;

public class GetPlatformCloudGatewaysResult extends CloudPlatformResult {
    private CloudGateWays cloudGateWays;

    public GetPlatformCloudGatewaysResult(Long resourceId, CloudGateWays cloudGateWays) {
        super(resourceId);
        this.cloudGateWays = cloudGateWays;
    }

    public GetPlatformCloudGatewaysResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudGateWays getCloudGateWays() {
        return cloudGateWays;
    }
}