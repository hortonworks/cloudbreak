package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class UpdateImageResult extends CloudPlatformResult<UpdateImageRequest<?>> {

    public UpdateImageResult() {
    }

    public UpdateImageResult(UpdateImageRequest<?> request) {
        super(request);
    }

    public UpdateImageResult(String statusReason, Exception errorDetails, UpdateImageRequest<?> request) {
        super(statusReason, errorDetails, request);
    }
}
