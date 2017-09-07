package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DownscaleStackResult extends CloudPlatformResult<CloudPlatformRequest> {

    private final List<CloudResource> downscaledResources;

    public DownscaleStackResult(CloudPlatformRequest<?> request, List<CloudResource> downscaledResources) {
        super(request);
        this.downscaledResources = ImmutableList.copyOf(downscaledResources);
    }

    public DownscaleStackResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
        downscaledResources = Collections.emptyList();
    }

    public List<CloudResource> getDownscaledResources() {
        return downscaledResources;
    }
}
