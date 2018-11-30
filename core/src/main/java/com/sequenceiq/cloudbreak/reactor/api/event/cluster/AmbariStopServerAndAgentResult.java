package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariStopServerAndAgentResult extends AbstractClusterScaleResult<AmbariStopServerAndAgentRequest> {
    public AmbariStopServerAndAgentResult(AmbariStopServerAndAgentRequest request) {
        super(request);
    }

    public AmbariStopServerAndAgentResult(String statusReason, Exception errorDetails, AmbariStopServerAndAgentRequest request) {
        super(statusReason, errorDetails, request);
    }
}
