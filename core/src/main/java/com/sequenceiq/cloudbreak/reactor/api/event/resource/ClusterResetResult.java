package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterResetResult extends ClusterPlatformResult<ClusterResetRequest> implements FlowPayload {
    public ClusterResetResult(ClusterResetRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterResetResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterResetRequest request) {
        super(statusReason, errorDetails, request);
    }
}
