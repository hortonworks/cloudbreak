package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class PrepareImageResult extends CloudPlatformResult implements FlowPayload {

    public PrepareImageResult(Long resourceId) {
        super(resourceId);
    }

    public PrepareImageResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    @JsonCreator
    public PrepareImageResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

}
