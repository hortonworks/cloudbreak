package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class GetInstancesStateResult extends CloudPlatformResult implements FlowPayload {

    private final List<CloudVmInstanceStatus> statuses;

    public GetInstancesStateResult(Long resourceId) {
        super(resourceId);
        statuses = Collections.emptyList();
    }

    public GetInstancesStateResult(Long resourceId, List<CloudVmInstanceStatus> statuses) {
        super(resourceId);
        this.statuses = statuses;
    }

    @JsonCreator
    public GetInstancesStateResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
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
