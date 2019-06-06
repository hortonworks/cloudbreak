package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DownscaleStackResult extends CloudPlatformResult {

    private final List<CloudResource> downscaledResources;

    public DownscaleStackResult(Long resourceId, Collection<CloudResource> downscaledResources) {
        super(resourceId);
        this.downscaledResources = ImmutableList.copyOf(downscaledResources);
    }

    public DownscaleStackResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        downscaledResources = Collections.emptyList();
    }

    public List<CloudResource> getDownscaledResources() {
        return downscaledResources;
    }
}
