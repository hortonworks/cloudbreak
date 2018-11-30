package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariStartServerAndAgentResult extends AbstractClusterScaleResult<AmbariStartServerAndAgentRequest> {
    public AmbariStartServerAndAgentResult(AmbariStartServerAndAgentRequest request) {
        super(request);
    }

    public AmbariStartServerAndAgentResult(String statusReason, Exception errorDetails, AmbariStartServerAndAgentRequest request) {
        super(statusReason, errorDetails, request);
    }
}
