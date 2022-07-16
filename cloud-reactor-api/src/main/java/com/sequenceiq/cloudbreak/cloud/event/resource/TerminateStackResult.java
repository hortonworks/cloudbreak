package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class TerminateStackResult extends CloudPlatformResult implements FlowPayload {

    public TerminateStackResult(Long resourceId) {
        super(resourceId);
    }

    @JsonCreator
    public TerminateStackResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
