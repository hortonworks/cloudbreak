package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class RebootInstancesResult extends CloudPlatformResult {

    private InstancesStatusResult results;

    private List<String> instanceIds;

    public RebootInstancesResult(Long resourceId, InstancesStatusResult results) {
        super(resourceId);
        this.results = results;
    }

    public RebootInstancesResult(String statusReason, Exception errorDetails, Long resourceId, List<String> instanceIds) {
        super(statusReason, errorDetails, resourceId);
        this.instanceIds = instanceIds;
    }

    public InstancesStatusResult getResults() {
        return results;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
