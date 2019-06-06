package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class PlatformParametersResult extends CloudPlatformResult {

    private Map<Platform, PlatformParameters> platformParameters;

    public PlatformParametersResult(Long resourceId, Map<Platform, PlatformParameters> platformParameters) {
        super(resourceId);
        this.platformParameters = platformParameters;
    }

    public PlatformParametersResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return platformParameters;
    }
}
