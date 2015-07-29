package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class CollectMetadataResult {

    private CloudContext cloudContext;

    private List<CloudVmInstanceStatus> results;

    public CollectMetadataResult(CloudContext cloudContext, List<CloudVmInstanceStatus> results) {
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public List<CloudVmInstanceStatus> getResults() {
        return results;
    }


    //BEGIN GENERATED CODE

    @Override
    public String toString() {
        return "CollectMetadataResult{" +
                "cloudContext=" + cloudContext +
                ", results=" + results +
                '}';
    }


    //END GENERATED CODE

}
