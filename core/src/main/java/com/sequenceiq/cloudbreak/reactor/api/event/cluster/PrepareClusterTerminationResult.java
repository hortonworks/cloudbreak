package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class PrepareClusterTerminationResult extends ClusterPlatformResult<PrepareClusterTerminationRequest> implements FlowPayload {

    public PrepareClusterTerminationResult(PrepareClusterTerminationRequest request) {
        super(request);
    }

    @JsonCreator
    public PrepareClusterTerminationResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") PrepareClusterTerminationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
