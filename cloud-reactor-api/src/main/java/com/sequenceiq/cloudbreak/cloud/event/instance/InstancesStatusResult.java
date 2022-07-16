package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class InstancesStatusResult {

    private final CloudContext cloudContext;

    private final List<CloudVmInstanceStatus> results;

    @JsonCreator
    public InstancesStatusResult(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("results") List<CloudVmInstanceStatus> results) {
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public List<CloudVmInstanceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InstancesStatusResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", results=").append(results);
        sb.append('}');
        return sb.toString();
    }
}
