package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscaleClusterResult extends AbstractClusterScaleResult<UpscaleClusterRequest> implements FlowPayload {

    public UpscaleClusterResult(UpscaleClusterRequest request) {
        super(request);
    }

    @JsonCreator
    public UpscaleClusterResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") UpscaleClusterRequest request) {
        super(statusReason, errorDetails, request);
    }
}
