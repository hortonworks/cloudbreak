package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerInstallComponentsResult extends AbstractClusterScaleResult<ClusterManagerInstallComponentsRequest> implements FlowPayload {
    public ClusterManagerInstallComponentsResult(ClusterManagerInstallComponentsRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterManagerInstallComponentsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterManagerInstallComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
