package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class PrepareClusterTerminationResult extends ClusterPlatformResult<PrepareClusterTerminationRequest> {

    public PrepareClusterTerminationResult(PrepareClusterTerminationRequest request) {
        super(request);
    }

    public PrepareClusterTerminationResult(String statusReason, Exception errorDetails, PrepareClusterTerminationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
