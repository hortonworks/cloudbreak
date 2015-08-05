package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class StartInstancesResult {

    private CloudContext cloudContext;

    private String statusReason;

    private List<CloudResourceStatus> results;

    public StartInstancesResult(CloudContext cloudContext, String statusReason) {
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

    //BEGIN GENERATED CODE

    @Override
    public String toString() {
        return "StartStackResult{" +
                "cloudContext=" + cloudContext +
                ", statusReason='" + statusReason + '\'' +
                ", results=" + results +
                '}';
    }


    //END GENERATED CODE
}
