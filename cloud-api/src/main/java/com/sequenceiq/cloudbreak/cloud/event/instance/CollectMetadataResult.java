package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class CollectMetadataResult {

    private CloudContext cloudContext;

    private ResourceStatus status;

    private String statusReason;

    private List<CloudResourceStatus> results;

    public CollectMetadataResult(CloudContext cloudContext, ResourceStatus status, String statusReason, List<CloudResourceStatus> results) {
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


    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "LaunchStackResult{" +
                "stackContext=" + cloudContext +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", results=" + results +
                '}';
    }
    //END GENERATED CODE
}
