package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class DownscaleStackCollectResourcesResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    private final Object resourcesToScale;

    public DownscaleStackCollectResourcesResult(CloudPlatformRequest<?> request, Object resourcesToScale) {
        super(request);
        this.resourcesToScale = resourcesToScale;
    }

    public DownscaleStackCollectResourcesResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
        resourcesToScale = Collections.emptyMap();
    }

    public Object getResourcesToScale() {
        return resourcesToScale;
    }
}
