package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class TerminateStackResult extends CloudPlatformResult {

    public TerminateStackResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public TerminateStackResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }
}
