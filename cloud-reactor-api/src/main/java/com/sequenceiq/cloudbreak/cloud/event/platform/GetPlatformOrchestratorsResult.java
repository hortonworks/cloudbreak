package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;

public class GetPlatformOrchestratorsResult extends CloudPlatformResult {
    private PlatformOrchestrators platformOrchestrators;

    public GetPlatformOrchestratorsResult(Long resourceId, PlatformOrchestrators platformOrchestrators) {
        super(resourceId);
        this.platformOrchestrators = platformOrchestrators;
    }

    public GetPlatformOrchestratorsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public PlatformOrchestrators getPlatformOrchestrators() {
        return platformOrchestrators;
    }
}
