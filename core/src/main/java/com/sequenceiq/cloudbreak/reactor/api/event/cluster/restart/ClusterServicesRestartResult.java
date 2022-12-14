package com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterServicesRestartResult extends ClusterPlatformResult<ClusterServicesRestartRequest> implements FlowPayload {

    public ClusterServicesRestartResult(ClusterServicesRestartRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterServicesRestartResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterServicesRestartRequest request) {
        super(statusReason, errorDetails, request);
    }
}
