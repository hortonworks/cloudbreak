package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class PreProvisionCheckResult extends CloudPlatformResult {

    public PreProvisionCheckResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

}
