package com.sequenceiq.cloudbreak.cloud.event.instance;


import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class StartInstancesResult extends CloudPlatformResult {

    private InstancesStatusResult results;

    public StartInstancesResult(Long resourceId, InstancesStatusResult results) {
        super(resourceId);
        this.results = results;
    }

    public StartInstancesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public InstancesStatusResult getResults() {
        return results;
    }
}
