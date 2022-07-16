package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class BootstrapNewNodesResult extends AbstractClusterBootstrapResult<BootstrapNewNodesRequest> implements FlowPayload {
    public BootstrapNewNodesResult(BootstrapNewNodesRequest request) {
        super(request);
    }

    @JsonCreator
    public BootstrapNewNodesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") BootstrapNewNodesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
