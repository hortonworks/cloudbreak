package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class GetInstancesStateResult {

    private final CloudContext cloudContext;
    private final List<CloudVmInstanceStatus> statuses;
    private final Exception exception;

    public GetInstancesStateResult(CloudContext cloudContext, List<CloudVmInstanceStatus> statuses) {
        this.cloudContext = cloudContext;
        this.statuses = statuses;
        this.exception = null;
    }

    public GetInstancesStateResult(CloudContext cloudContext, Exception exception) {
        this.cloudContext = cloudContext;
        this.exception = exception;
        this.statuses = Collections.emptyList();
    }

    public Exception getException() {
        return exception;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public List<CloudVmInstanceStatus> getStatuses() {
        return statuses;
    }

    public boolean isFailed() {
        return exception != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetInstancesStateResult{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", statuses=").append(statuses);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
