package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerInitComponentsResult extends AbstractClusterScaleResult<ClusterManagerInitComponentsRequest> implements FlowPayload {

    public ClusterManagerInitComponentsResult(ClusterManagerInitComponentsRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterManagerInitComponentsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterManagerInitComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
