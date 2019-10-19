package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StopClusterComponentsResult extends AbstractClusterScaleResult<ClusterManagerStopComponentsRequest> {

    public StopClusterComponentsResult(ClusterManagerStopComponentsRequest request) {
        super(request);
    }

    public StopClusterComponentsResult(String statusReason, Exception errorDetails, ClusterManagerStopComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
