package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public class ValidateImageResult extends CloudPlatformResult implements FlowPayload {

    private StatedImage image;

    public ValidateImageResult(Long resourceId) {
        super(resourceId);
    }

    public ValidateImageResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    @JsonCreator
    public ValidateImageResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public ValidateImageResult(Long resourceId, StatedImage image) {
        super(resourceId);
        this.image = image;
    }

    public StatedImage getImage() {
        return image;
    }

    public void setImage(StatedImage image) {
        this.image = image;
    }
}
