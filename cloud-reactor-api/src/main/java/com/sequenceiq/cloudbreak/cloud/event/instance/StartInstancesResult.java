package com.sequenceiq.cloudbreak.cloud.event.instance;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class StartInstancesResult extends CloudPlatformResult implements FlowPayload {

    private final InstancesStatusResult results;

    public StartInstancesResult(Long resourceId, InstancesStatusResult results) {
        super(resourceId);
        this.results = results;
    }

    @JsonCreator
    public StartInstancesResult(
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
