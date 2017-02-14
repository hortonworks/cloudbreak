package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;

public class GetPlatformImagesResult extends CloudPlatformResult<CloudPlatformRequest> {
    private PlatformImages platformImages;

    public GetPlatformImagesResult(CloudPlatformRequest<?> request, PlatformImages platformImages) {
        super(request);
        this.platformImages = platformImages;
    }

    public GetPlatformImagesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public PlatformImages getPlatformImages() {
        return platformImages;
    }
}
