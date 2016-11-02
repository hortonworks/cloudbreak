package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class InstancesStatusResult {

    private final CloudContext cloudContext;

    private final List<CloudVmInstanceStatus> results;

    public InstancesStatusResult(CloudContext cloudContext, List<CloudVmInstanceStatus> results) {
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
        final StringBuilder sb = new StringBuilder("InstancesStatusResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", results=").append(results);
        sb.append('}');
        return sb.toString();
    }
}