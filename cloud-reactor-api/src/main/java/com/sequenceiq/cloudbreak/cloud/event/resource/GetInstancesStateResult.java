package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class GetInstancesStateResult extends CloudPlatformResult {

    private final List<CloudVmInstanceStatus> statuses;

    public GetInstancesStateResult(Long resourceId) {
        super(resourceId);
        statuses = Collections.emptyList();
    }

    public GetInstancesStateResult(Long resourceId, List<CloudVmInstanceStatus> statuses) {
        super(resourceId);
        this.statuses = statuses;
    }

    public GetInstancesStateResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        statuses = Collections.emptyList();
    }

    public List<CloudVmInstanceStatus> getStatuses() {
        return statuses;
    }

    public boolean isFailed() {
        return getErrorDetails() != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GetInstancesStateResult{");
        sb.append(", statuses=").append(statuses);
        sb.append(", exception=").append(getErrorDetails());
        sb.append('}');
        return sb.toString();
    }
}
