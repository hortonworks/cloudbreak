package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;

public class CheckImageResult extends CloudPlatformResult<CloudPlatformRequest> {

    private final ImageStatus imageStatus;

    private final Integer statusProgressValue;

    public CheckImageResult(CloudPlatformRequest<?> request, ImageStatus imageStatus, Integer statusProgressValue) {
        super(request);
        this.imageStatus = imageStatus;
        this.statusProgressValue = statusProgressValue;
    }

    public CheckImageResult(Exception errorDetails, CloudPlatformRequest<?> request, ImageStatus imageStatus) {
        this(errorDetails.getMessage(), errorDetails, request, imageStatus);
    }

    public CheckImageResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request, ImageStatus imageStatus) {
        super(statusReason, errorDetails, request);
        this.imageStatus = imageStatus;
        this.statusProgressValue = null;
    }

    public ImageStatus getImageStatus() {
        return imageStatus;
    }

    public Integer getStatusProgressValue() {
        return statusProgressValue;
    }
}
