package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class TerminateStackResult extends CloudPlatformResult<TerminateStackRequest<TerminateStackResult>> {

    public TerminateStackResult(TerminateStackRequest<TerminateStackResult> request) {
        super(request);
    }

    public TerminateStackResult(String statusReason, Exception errorDetails, TerminateStackRequest<TerminateStackResult> request) {
        super(statusReason, errorDetails, request);
    }
}
