package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerStartComponentsResult extends AbstractClusterScaleResult<ClusterManagerStartComponentsRequest> {
    public ClusterManagerStartComponentsResult(ClusterManagerStartComponentsRequest request) {
        super(request);
    }

    public ClusterManagerStartComponentsResult(String statusReason, Exception errorDetails, ClusterManagerStartComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
