package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class StopStartUpscaleStartInstancesResult extends CloudPlatformResult implements FlowPayload {

    private final StopStartUpscaleStartInstancesRequest startInstanceRequest;

    private final List<CloudVmInstanceStatus> affectedInstanceStatuses;

    public StopStartUpscaleStartInstancesResult(Long resourceId, StopStartUpscaleStartInstancesRequest request,
            List<CloudVmInstanceStatus> affectedInstanceStatuses) {
        super(resourceId);
        this.startInstanceRequest = request;
        this.affectedInstanceStatuses = affectedInstanceStatuses;
    }

    @JsonCreator
    public StopStartUpscaleStartInstancesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("startInstanceRequest") StopStartUpscaleStartInstancesRequest request) {
        super(statusReason, errorDetails, resourceId);
        this.startInstanceRequest = request;
        this.affectedInstanceStatuses = Collections.emptyList();
    }

    public StopStartUpscaleStartInstancesRequest getStartInstanceRequest() {
        return startInstanceRequest;
    }

    public List<CloudVmInstanceStatus> getAffectedInstanceStatuses() {
        return affectedInstanceStatuses;
    }
}
