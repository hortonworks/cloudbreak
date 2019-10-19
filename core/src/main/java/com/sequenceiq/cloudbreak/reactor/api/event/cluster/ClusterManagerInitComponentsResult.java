package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterManagerInitComponentsResult extends AbstractClusterScaleResult<ClusterManagerInitComponentsRequest> {

    public ClusterManagerInitComponentsResult(ClusterManagerInitComponentsRequest request) {
        super(request);
    }

    public ClusterManagerInitComponentsResult(String statusReason, Exception errorDetails, ClusterManagerInitComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
