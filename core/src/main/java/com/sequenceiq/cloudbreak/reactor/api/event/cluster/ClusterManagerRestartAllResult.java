package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerRestartAllResult extends AbstractClusterScaleResult<AmbariRestartAllRequest> {
    public ClusterManagerRestartAllResult(AmbariRestartAllRequest request) {
        super(request);
    }

    public ClusterManagerRestartAllResult(String statusReason, Exception errorDetails, AmbariRestartAllRequest request) {
        super(statusReason, errorDetails, request);
    }
}
