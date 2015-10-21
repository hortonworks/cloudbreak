package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

public class StartInstancesResult {

    private final CloudContext cloudContext;
    private final InstancesStatusResult results;
    private final Exception exception;

    public StartInstancesResult(CloudContext cloudContext, InstancesStatusResult results) {
        this.cloudContext = cloudContext;
        this.results = results;
        this.exception = null;
    }

    public StartInstancesResult(CloudContext cloudContext, Exception exception) {
        this.exception = exception;
        this.cloudContext = cloudContext;
        this.results = null;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public InstancesStatusResult getResults() {
        return results;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isFailed() {
        return exception != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StartInstancesResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", results=").append(results);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
