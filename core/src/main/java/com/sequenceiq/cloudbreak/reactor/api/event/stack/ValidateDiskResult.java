package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class ValidateDiskResult extends CloudPlatformResult implements FlowPayload {

    public ValidateDiskResult(Long resourceId) {
        super(resourceId);
    }

    public ValidateDiskResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    @JsonCreator
    public ValidateDiskResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

}
