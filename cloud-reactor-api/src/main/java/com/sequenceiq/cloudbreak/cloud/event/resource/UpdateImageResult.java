package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class UpdateImageResult extends CloudPlatformResult implements FlowPayload {

    public UpdateImageResult(Long resourceId) {
        super(resourceId);
    }

    @JsonCreator
    public UpdateImageResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
