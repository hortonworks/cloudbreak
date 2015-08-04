package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class StartStackResult {

    private CloudContext cloudContext;

    private String statusReason;

    private List<CloudResourceStatus> results;

    public StartStackResult(CloudContext cloudContext, String statusReason) {
        this.cloudContext = cloudContext;
        this.statusReason = statusReason;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StopStackResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", statusReason='").append(statusReason).append('\'');
        sb.append(", results=").append(results);
        sb.append('}');
        return sb.toString();
    }
}
