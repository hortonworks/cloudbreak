package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class PlatformParameterResult extends CloudPlatformResult<CloudPlatformRequest> {

    private PlatformParameters platformParameters;

    public PlatformParameterResult(CloudPlatformRequest<?> request, PlatformParameters platformParameters) {
        super(request);
        this.platformParameters = platformParameters;
    }

    public PlatformParameterResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformParameters getPlatformParameters() {
        return platformParameters;
    }
}
