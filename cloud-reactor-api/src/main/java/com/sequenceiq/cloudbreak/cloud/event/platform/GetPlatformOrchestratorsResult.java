package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;

public class GetPlatformOrchestratorsResult extends CloudPlatformResult<CloudPlatformRequest> {
    private PlatformOrchestrators platformOrchestrators;

    public GetPlatformOrchestratorsResult(CloudPlatformRequest<?> request, PlatformOrchestrators platformOrchestrators) {
        super(request);
        this.platformOrchestrators = platformOrchestrators;
    }

    public GetPlatformOrchestratorsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformOrchestrators getPlatformOrchestrators() {
        return platformOrchestrators;
    }
}
