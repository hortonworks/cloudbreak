package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class StopInstancesResult extends CloudPlatformResult {

    private InstancesStatusResult results;

    public StopInstancesResult(Long resourceId, InstancesStatusResult results) {
        super(resourceId);
        this.results = results;
    }

    public StopInstancesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public InstancesStatusResult getResults() {
        return results;
    }
}
