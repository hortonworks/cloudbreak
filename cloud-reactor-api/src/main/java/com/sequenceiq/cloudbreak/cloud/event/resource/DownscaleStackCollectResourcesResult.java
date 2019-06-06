package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class DownscaleStackCollectResourcesResult extends CloudPlatformResult {

    private final Object resourcesToScale;

    public DownscaleStackCollectResourcesResult(Long resourceId, Object resourcesToScale) {
        super(resourceId);
        this.resourcesToScale = resourcesToScale;
    }

    public DownscaleStackCollectResourcesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        resourcesToScale = Collections.emptyMap();
    }

    public Object getResourcesToScale() {
        return resourcesToScale;
    }
}
