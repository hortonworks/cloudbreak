package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariStopComponentsResult extends AbstractClusterScaleResult<AmbariStopComponentsRequest> {

    public AmbariStopComponentsResult(AmbariStopComponentsRequest request) {
        super(request);
    }

    public AmbariStopComponentsResult(String statusReason, Exception errorDetails, AmbariStopComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
