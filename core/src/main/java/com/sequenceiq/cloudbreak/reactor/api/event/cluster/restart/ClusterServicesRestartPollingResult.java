package com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingRequest;

public class ClusterServicesRestartPollingResult extends ClusterPlatformResult<ClusterStartPollingRequest> implements FlowPayload {

    public ClusterServicesRestartPollingResult(ClusterStartPollingRequest request) {
        super(request);
    }

    @JsonCreator
    public ClusterServicesRestartPollingResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") ClusterStartPollingRequest request) {
        super(statusReason, errorDetails, request);
    }
}
