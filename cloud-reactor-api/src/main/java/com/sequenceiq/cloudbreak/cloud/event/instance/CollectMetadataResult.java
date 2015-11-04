package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

public class CollectMetadataResult {

    private CloudContext cloudContext;
    private List<CloudVmMetaDataStatus> results;
    private Exception exception;

    public CollectMetadataResult(CloudContext cloudContext, List<CloudVmMetaDataStatus> results) {
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

    public List<CloudVmMetaDataStatus> getResults() {
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
