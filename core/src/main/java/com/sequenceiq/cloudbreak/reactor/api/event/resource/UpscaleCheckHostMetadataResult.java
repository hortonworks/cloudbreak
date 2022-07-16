package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class UpscaleCheckHostMetadataResult extends ClusterPlatformResult<UpscaleCheckHostMetadataRequest> implements FlowPayload {
    public UpscaleCheckHostMetadataResult(UpscaleCheckHostMetadataRequest request) {
        super(request);
    }

    @JsonCreator
    public UpscaleCheckHostMetadataResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") UpscaleCheckHostMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }
}
