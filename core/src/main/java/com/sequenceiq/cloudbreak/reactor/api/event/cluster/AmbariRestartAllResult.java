package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariRestartAllResult extends AbstractClusterScaleResult<AmbariRestartAllRequest> {
    public AmbariRestartAllResult(AmbariRestartAllRequest request) {
        super(request);
    }

    public AmbariRestartAllResult(String statusReason, Exception errorDetails, AmbariRestartAllRequest request) {
        super(statusReason, errorDetails, request);
    }
}
