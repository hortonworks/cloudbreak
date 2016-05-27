package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class UpdateStackResult implements Payload {

    private CloudContext cloudContext;
    private ResourceStatus status;
    private String statusReason;
    private Exception exception;
    private List<CloudResourceStatus> results;

    public UpdateStackResult(CloudContext cloudContext, ResourceStatus status, String statusReason, List<CloudResourceStatus> results) {
        this.cloudContext = cloudContext;
        this.status = status;
        this.statusReason = statusReason;
        this.results = results;
    }

    public UpdateStackResult(CloudContext cloudContext, Exception exception) {
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

    public String getStatusReason() {
        return statusReason;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    public boolean isFailed() {
        return status == ResourceStatus.FAILED;
    }

    @Override
    public Long getStackId() {
        return cloudContext.getId();
    }

    @Override
    public String toString() {
        return "LaunchStackResult{"
                + "stackContext=" + cloudContext
                + ", status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", results=" + results
                + '}';
    }
}
