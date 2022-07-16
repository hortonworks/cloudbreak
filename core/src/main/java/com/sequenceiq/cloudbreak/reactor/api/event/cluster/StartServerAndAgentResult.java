package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StartServerAndAgentResult extends AbstractClusterScaleResult<AmbariStartServerAndAgentRequest> implements FlowPayload {
    public StartServerAndAgentResult(AmbariStartServerAndAgentRequest request) {
        super(request);
    }

    @JsonCreator
    public StartServerAndAgentResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") AmbariStartServerAndAgentRequest request) {
        super(statusReason, errorDetails, request);
    }
}
