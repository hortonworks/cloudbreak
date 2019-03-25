package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StartServerAndAgentResult extends AbstractClusterScaleResult<AmbariStartServerAndAgentRequest> {
    public StartServerAndAgentResult(AmbariStartServerAndAgentRequest request) {
        super(request);
    }

    public StartServerAndAgentResult(String statusReason, Exception errorDetails, AmbariStartServerAndAgentRequest request) {
        super(statusReason, errorDetails, request);
    }
}
