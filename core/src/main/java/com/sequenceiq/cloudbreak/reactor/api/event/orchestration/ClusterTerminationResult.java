package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterTerminationResult extends ClusterPlatformResult<ClusterTerminationRequest> implements FlowPayload {

    public ClusterTerminationResult(ClusterTerminationRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterTerminationResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterTerminationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
