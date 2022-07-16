package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerEnsureComponentsAreStoppedResult
        extends AbstractClusterScaleResult<EnsureClusterComponentsAreStoppedRequest> implements FlowPayload {
    public ClusterManagerEnsureComponentsAreStoppedResult(EnsureClusterComponentsAreStoppedRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterManagerEnsureComponentsAreStoppedResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") EnsureClusterComponentsAreStoppedRequest request) {
        super(statusReason, errorDetails, request);
    }
}
