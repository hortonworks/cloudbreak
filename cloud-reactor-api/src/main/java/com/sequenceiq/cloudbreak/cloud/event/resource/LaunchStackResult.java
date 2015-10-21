package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class LaunchStackResult {

    private CloudContext cloudContext;
    private Exception exception;
    private List<CloudResourceStatus> results;

    public LaunchStackResult(CloudContext cloudContext, List<CloudResourceStatus> results) {
        this.cloudContext = cloudContext;
        this.results = results;
    }

    public LaunchStackResult(CloudContext cloudContext, Exception exception) {
        this.cloudContext = cloudContext;
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public boolean isFailed() {
        return exception != null;
    }

    public List<CloudResourceStatus> getResults() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LaunchStackResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", exception=").append(exception);
        sb.append(", results=").append(results);
        sb.append('}');
        return sb.toString();
    }
}
