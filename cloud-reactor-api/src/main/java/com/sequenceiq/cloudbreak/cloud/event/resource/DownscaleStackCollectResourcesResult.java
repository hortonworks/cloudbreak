package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class DownscaleStackCollectResourcesResult extends CloudPlatformResult implements FlowPayload {

    private final Object resourcesToScale;

    public DownscaleStackCollectResourcesResult(Long resourceId, Object resourcesToScale) {
        super(resourceId);
        this.resourcesToScale = resourcesToScale;
    }

    @JsonCreator
    public DownscaleStackCollectResourcesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        resourcesToScale = Collections.emptyMap();
    }

    public Object getResourcesToScale() {
        return resourcesToScale;
    }
}
