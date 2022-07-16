package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.common.api.type.ImageStatus;

public class CheckImageResult extends CloudPlatformResult implements FlowPayload {

    private final ImageStatus imageStatus;

    private final Integer statusProgressValue;

    public CheckImageResult(Long resourceId, ImageStatus imageStatus, Integer statusProgressValue) {
        super(resourceId);
        this.imageStatus = imageStatus;
        this.statusProgressValue = statusProgressValue;
    }

    public CheckImageResult(Exception errorDetails, Long resourceId, ImageStatus imageStatus) {
        this(errorDetails.getMessage(), errorDetails, resourceId, imageStatus);
    }

    @JsonCreator
    public CheckImageResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageStatus") ImageStatus imageStatus) {
        super(statusReason, errorDetails, resourceId);
        this.imageStatus = imageStatus;
        statusProgressValue = null;
    }

    public ImageStatus getImageStatus() {
        return imageStatus;
    }

    public Integer getStatusProgressValue() {
        return statusProgressValue;
    }
}
