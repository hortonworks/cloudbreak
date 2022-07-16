package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StopClusterComponentsResult extends AbstractClusterScaleResult<ClusterManagerStopComponentsRequest> implements FlowPayload {

    public StopClusterComponentsResult(ClusterManagerStopComponentsRequest request) {
        super(request);
    }

    @JsonCreator
    public StopClusterComponentsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterManagerStopComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
