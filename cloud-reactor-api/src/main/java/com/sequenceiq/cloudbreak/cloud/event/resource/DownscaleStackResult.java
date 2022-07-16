package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class DownscaleStackResult extends CloudPlatformResult implements FlowPayload {

    private final List<CloudResource> downscaledResources;

    public DownscaleStackResult(Long resourceId, Collection<CloudResource> downscaledResources) {
        super(resourceId);
        this.downscaledResources = ImmutableList.copyOf(downscaledResources);
    }

    @JsonCreator
    public DownscaleStackResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        downscaledResources = Collections.emptyList();
    }

    public List<CloudResource> getDownscaledResources() {
        return downscaledResources;
    }
}
