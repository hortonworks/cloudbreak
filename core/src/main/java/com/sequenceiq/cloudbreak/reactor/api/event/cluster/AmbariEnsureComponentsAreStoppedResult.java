package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariEnsureComponentsAreStoppedResult  extends AbstractClusterScaleResult<EnsureClusterComponentsAreStoppedRequest> {
    public AmbariEnsureComponentsAreStoppedResult(EnsureClusterComponentsAreStoppedRequest request) {
        super(request);
    }

    public AmbariEnsureComponentsAreStoppedResult(String statusReason, Exception errorDetails, EnsureClusterComponentsAreStoppedRequest request) {
        super(statusReason, errorDetails, request);
    }
}
