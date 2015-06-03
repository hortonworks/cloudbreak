package com.sequenceiq.cloudbreak.cloud.event;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class LaunchStackResult {

    private StackContext stackContext;

    private ResourceStatus status;

    private String statusReason;

    private List<CloudResourceStatus> results;

    public LaunchStackResult(StackContext stackContext, ResourceStatus status, String statusReason, List<CloudResourceStatus> results) {
        this.stackContext = stackContext;
        this.status = status;
        this.statusReason = statusReason;
        this.results = results;
    }

    public StackContext getStackContext() {
        return stackContext;
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
                "stackContext=" + stackContext +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", results=" + results +
                '}';
    }
    //END GENERATED CODE
}
