package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;

public class GetPlatformCloudGatewaysResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private CloudGateWays cloudGateWays;

    public GetPlatformCloudGatewaysResult(CloudPlatformRequest<?> request, CloudGateWays cloudGateWays) {
        super(request);
        this.cloudGateWays = cloudGateWays;
    }

    public GetPlatformCloudGatewaysResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudGateWays getCloudGateWays() {
        return cloudGateWays;
    }
}