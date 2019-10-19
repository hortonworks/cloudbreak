package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerEnsureComponentsAreStoppedResult extends AbstractClusterScaleResult<EnsureClusterComponentsAreStoppedRequest> {
    public ClusterManagerEnsureComponentsAreStoppedResult(EnsureClusterComponentsAreStoppedRequest request) {
        super(request);
    }

    public ClusterManagerEnsureComponentsAreStoppedResult(String statusReason, Exception errorDetails, EnsureClusterComponentsAreStoppedRequest request) {
        super(statusReason, errorDetails, request);
    }
}
