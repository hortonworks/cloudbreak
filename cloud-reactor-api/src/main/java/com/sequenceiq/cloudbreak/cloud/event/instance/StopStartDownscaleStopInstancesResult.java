package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class StopStartDownscaleStopInstancesResult extends CloudPlatformResult implements FlowPayload {

    private final StopStartDownscaleStopInstancesRequest stopInstancesRequest;

    private final List<CloudVmInstanceStatus> affectedInstanceStatuses;

    public StopStartDownscaleStopInstancesResult(Long resourceId, StopStartDownscaleStopInstancesRequest request,
            List<CloudVmInstanceStatus> affectedInstanceStatuses) {
        super(resourceId);
        this.stopInstancesRequest = request;
        this.affectedInstanceStatuses = affectedInstanceStatuses;
    }

    @JsonCreator
    public StopStartDownscaleStopInstancesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("stopInstancesRequest") StopStartDownscaleStopInstancesRequest request) {
        super(statusReason, errorDetails, resourceId);
        this.stopInstancesRequest = request;
        this.affectedInstanceStatuses = Collections.emptyList();
    }

    public StopStartDownscaleStopInstancesRequest getStopInstancesRequest() {
        return stopInstancesRequest;
    }

    public List<CloudVmInstanceStatus> getAffectedInstanceStatuses() {
        return affectedInstanceStatuses;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleStopInstancesResult{" +
                "stopInstancesRequest=" + stopInstancesRequest +
                ", affectedInstanceStatuses=" + affectedInstanceStatuses +
                '}';
    }
}
