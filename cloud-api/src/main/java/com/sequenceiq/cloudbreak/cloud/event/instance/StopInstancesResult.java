package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public class StopInstancesResult {

    private final CloudContext cloudContext;
    private final InstancesStatusResult results;
    private final Exception exception;

    public StopInstancesResult(CloudContext cloudContext, InstancesStatusResult results) {
        this.cloudContext = cloudContext;
        this.results = results;
        this.exception = null;
    }

    public StopInstancesResult(CloudContext cloudContext, Exception exception) {
        this.cloudContext = cloudContext;
        this.results = null;
        this.exception = exception;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isFailed() {
        return exception != null;
    }

    public InstancesStatusResult getResults() {
        return results;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StopInstancesResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", results=").append(results);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
