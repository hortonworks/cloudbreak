package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class CollectMetadataResult {

    private CloudContext cloudContext;
    private List<CloudVmInstanceStatus> results;
    private Exception exception;

    public CollectMetadataResult(CloudContext cloudContext, List<CloudVmInstanceStatus> results) {
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public CollectMetadataResult(CloudContext cloudContext, Exception exception) {
        this.cloudContext = cloudContext;
        this.exception = exception;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public List<CloudVmInstanceStatus> getResults() {
        return results;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CollectMetadataResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", results=").append(results);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
