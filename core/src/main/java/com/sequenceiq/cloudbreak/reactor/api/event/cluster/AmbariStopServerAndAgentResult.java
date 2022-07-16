package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariStopServerAndAgentResult extends AbstractClusterScaleResult<AmbariStopServerAndAgentRequest> implements FlowPayload {
    public AmbariStopServerAndAgentResult(AmbariStopServerAndAgentRequest request) {
        super(request);
    }

    @JsonCreator
    public AmbariStopServerAndAgentResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") AmbariStopServerAndAgentRequest request) {
        super(statusReason, errorDetails, request);
    }
}
