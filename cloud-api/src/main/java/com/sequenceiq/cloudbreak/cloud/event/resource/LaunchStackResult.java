package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class LaunchStackResult {

    private CloudContext cloudContext;
    private ResourceStatus status;
    private String statusReason;
    private Exception exception;
    private List<CloudResourceStatus> results;

    public LaunchStackResult(CloudContext cloudContext, ResourceStatus status, String statusReason, List<CloudResourceStatus> results) {
        this.cloudContext = cloudContext;
        this.status = status;
        this.statusReason = statusReason;
        this.results = results;
    }

    public LaunchStackResult(CloudContext cloudContext, Exception exception) {
        this.cloudContext = cloudContext;
        this.exception = exception;
        this.status = ResourceStatus.FAILED;
    }

    public Exception getException() {
        return exception;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public boolean isFailed() {
        return status == ResourceStatus.FAILED;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LaunchStackResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", status=").append(status);
        sb.append(", statusReason='").append(statusReason).append('\'');
        sb.append(", exception=").append(exception);
        sb.append(", results=").append(results);
        sb.append('}');
        return sb.toString();
    }
}
