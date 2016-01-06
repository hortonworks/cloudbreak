package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class GetInstancesStateResult extends CloudPlatformResult<GetInstancesStateRequest<GetInstancesStateResult>> {

    private final List<CloudVmInstanceStatus> statuses;

    public GetInstancesStateResult(GetInstancesStateRequest<GetInstancesStateResult> request) {
        super(request);
        this.statuses = Collections.emptyList();
    }

    public GetInstancesStateResult(GetInstancesStateRequest<GetInstancesStateResult> request, List<CloudVmInstanceStatus> statuses) {
        super(request);
        this.statuses = statuses;
    }

    public GetInstancesStateResult(String statusReason, Exception errorDetails, GetInstancesStateRequest<GetInstancesStateResult> request) {
        super(statusReason, errorDetails, request);
        this.statuses = Collections.emptyList();
    }

    public CloudContext getCloudContext() {
        return getRequest().getCloudContext();
    }

    public List<CloudVmInstanceStatus> getStatuses() {
        return statuses;
    }

    public boolean isFailed() {
        return getErrorDetails() != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetInstancesStateResult{");
        sb.append("cloudContext=").append(getCloudContext());
        sb.append(", statuses=").append(statuses);
        sb.append(", exception=").append(getErrorDetails());
        sb.append('}');
        return sb.toString();
    }
}
