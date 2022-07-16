package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class StopInstancesResult extends CloudPlatformResult implements FlowPayload {

    private final InstancesStatusResult results;

    public StopInstancesResult(Long resourceId, InstancesStatusResult results) {
        super(resourceId);
        this.results = results;
    }

    @JsonCreator
    public StopInstancesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        this.results = null;
    }

    public InstancesStatusResult getResults() {
        return results;
    }
}
