package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterTerminationResult extends ClusterPlatformResult<ClusterTerminationRequest> {
    public ClusterTerminationResult(ClusterTerminationRequest request) {
        super(request);
    }

    public ClusterTerminationResult(String statusReason, Exception errorDetails, ClusterTerminationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
