package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariEnsureComponentsAreStoppedResult  extends AbstractClusterScaleResult<AmbariEnsureComponentsAreStoppedRequest> {
    public AmbariEnsureComponentsAreStoppedResult(AmbariEnsureComponentsAreStoppedRequest request) {
        super(request);
    }

    public AmbariEnsureComponentsAreStoppedResult(String statusReason, Exception errorDetails, AmbariEnsureComponentsAreStoppedRequest request) {
        super(statusReason, errorDetails, request);
    }
}
