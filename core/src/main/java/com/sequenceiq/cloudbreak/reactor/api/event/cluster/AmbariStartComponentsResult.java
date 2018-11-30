package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariStartComponentsResult  extends AbstractClusterScaleResult<AmbariStartComponentsRequest> {
    public AmbariStartComponentsResult(AmbariStartComponentsRequest request) {
        super(request);
    }

    public AmbariStartComponentsResult(String statusReason, Exception errorDetails, AmbariStartComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
