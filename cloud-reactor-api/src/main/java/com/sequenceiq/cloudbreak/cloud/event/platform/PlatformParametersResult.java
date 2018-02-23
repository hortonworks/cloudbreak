package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class PlatformParametersResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    private Map<Platform, PlatformParameters> platformParameters;

    public PlatformParametersResult(CloudPlatformRequest<?> request, Map<Platform, PlatformParameters> platformParameters) {
        super(request);
        this.platformParameters = platformParameters;
    }

    public PlatformParametersResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        return platformParameters;
    }
}
