package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class PrepareImageResult extends CloudPlatformResult implements FlowPayload {
    private final String imageIdentifier;

    public PrepareImageResult(Long resourceId, String imageIdentifier) {
        super(resourceId);
        this.imageIdentifier = imageIdentifier;
    }

    public PrepareImageResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId, null);
    }

    @JsonCreator
    public PrepareImageResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageIdentifier") String imageIdentifier) {
        super(statusReason, errorDetails, resourceId);
        this.imageIdentifier = imageIdentifier;
    }

    public String getImageIdentifier() {
        return imageIdentifier;
    }
}
