package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class TerminateStackResult extends CloudPlatformResult {

    public TerminateStackResult(Long resourceId) {
        super(resourceId);
    }

    public TerminateStackResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
