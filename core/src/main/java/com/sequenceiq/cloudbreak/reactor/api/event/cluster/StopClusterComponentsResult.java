package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StopClusterComponentsResult extends AbstractClusterScaleResult<AmbariStopComponentsRequest> {

    public StopClusterComponentsResult(AmbariStopComponentsRequest request) {
        super(request);
    }

    public StopClusterComponentsResult(String statusReason, Exception errorDetails, AmbariStopComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
