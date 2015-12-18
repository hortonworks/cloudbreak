package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class SetupResult extends CloudPlatformResult<CloudPlatformRequest> {

    public SetupResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public SetupResult(Exception errorDetails, CloudPlatformRequest<?> request) {
        this(errorDetails.getMessage(), errorDetails, request);
    }

    public SetupResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

}
