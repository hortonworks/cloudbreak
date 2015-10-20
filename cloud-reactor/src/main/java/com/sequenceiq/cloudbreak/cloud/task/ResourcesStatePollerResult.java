package com.sequenceiq.cloudbreak.cloud.task;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class ResourcesStatePollerResult {

    private CloudContext cloudContext;
    private ResourceStatus status;
    private String statusReason;
    private List<CloudResourceStatus> results;

    public ResourcesStatePollerResult(CloudContext cloudContext) {
        this.cloudContext = cloudContext;
        this.results = new ArrayList<>();
    }

    public ResourcesStatePollerResult(CloudContext cloudContext, ResourceStatus status, String statusReason, List<CloudResourceStatus> results) {
        this.cloudContext = cloudContext;
        this.status = status;
        this.statusReason = statusReason;
        this.results = results;
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

    public void setCloudContext(CloudContext cloudContext) {
        this.cloudContext = cloudContext;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public void addResults(List<CloudResourceStatus> results) {
        this.results.addAll(results);
    }

    @Override
    public String toString() {
        return "ResourcesStatePollerResult{"
                + "cloudContext=" + cloudContext
                + ", status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", results=" + results
                + '}';
    }
}
