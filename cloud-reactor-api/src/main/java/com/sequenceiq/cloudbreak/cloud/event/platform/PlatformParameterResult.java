package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class PlatformParameterResult extends CloudPlatformResult {

    private PlatformParameters platformParameters;

    public PlatformParameterResult(Long resourceId, PlatformParameters platformParameters) {
        super(resourceId);
        this.platformParameters = platformParameters;
    }

    public PlatformParameterResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public PlatformParameters getPlatformParameters() {
        return platformParameters;
    }
}
