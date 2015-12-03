package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DownscaleStackResult extends CloudPlatformResult<CloudPlatformRequest> {

    private List<CloudResource> downscaledResources;

    public DownscaleStackResult(CloudPlatformRequest<?> request, List<CloudResource> downscaledResources) {
        super(request);
        this.downscaledResources = downscaledResources;
    }

    public DownscaleStackResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
        this.downscaledResources = new ArrayList<>();
    }

    public List<CloudResource> getDownscaledResources() {
        return downscaledResources;
    }
}
