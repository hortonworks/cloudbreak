package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerRestartAllResult extends AbstractClusterScaleResult<AmbariRestartAllRequest> implements FlowPayload {
    public ClusterManagerRestartAllResult(AmbariRestartAllRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterManagerRestartAllResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") AmbariRestartAllRequest request) {
        super(statusReason, errorDetails, request);
    }
}
