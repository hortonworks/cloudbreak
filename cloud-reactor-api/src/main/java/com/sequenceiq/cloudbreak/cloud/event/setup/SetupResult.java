package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class SetupResult extends CloudPlatformResult implements FlowPayload {

    public SetupResult(Long resourceId) {
        super(resourceId);
    }

    public SetupResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    @JsonCreator
    public SetupResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

}
