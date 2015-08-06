package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public class StopInstancesResult {

    private final CloudContext cloudContext;
    private final String statusReason;
    private final InstancesStatusResult results;

    public StopInstancesResult(CloudContext cloudContext, String statusReason, InstancesStatusResult results) {
        this.cloudContext = cloudContext;
        this.statusReason = statusReason;
        this.results = results;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public InstancesStatusResult getResults() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StopInstancesResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", statusReason='").append(statusReason).append('\'');
        sb.append(", results=").append(results);
        sb.append('}');
        return sb.toString();
    }
}
