package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerStartComponentsResult extends AbstractClusterScaleResult<ClusterManagerStartComponentsRequest> implements FlowPayload {
    public ClusterManagerStartComponentsResult(ClusterManagerStartComponentsRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterManagerStartComponentsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterManagerStartComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
