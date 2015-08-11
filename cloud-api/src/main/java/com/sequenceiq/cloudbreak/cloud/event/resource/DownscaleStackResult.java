package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DownscaleStackResult extends CloudPlatformResult {

    private List<CloudResource> downscaledResources;

    public DownscaleStackResult(CloudPlatformRequest<?> request, List<CloudResource> downscaledResources) {
        super(request);
        this.downscaledResources = downscaledResources;
    }

    public DownscaleStackResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request, List<CloudResource> downscaledResources) {
        super(statusReason, errorDetails, request);
        this.downscaledResources = downscaledResources;
    }

    public List<CloudResource> getDownscaledResources() {
        return downscaledResources;
    }
}
