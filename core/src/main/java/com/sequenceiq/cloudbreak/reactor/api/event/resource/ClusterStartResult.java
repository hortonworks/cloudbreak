package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartResult extends ClusterPlatformResult<ClusterStartRequest> {
    public ClusterStartResult(ClusterStartRequest request) {
        super(request);
    }

    public ClusterStartResult(String statusReason, Exception errorDetails, ClusterStartRequest request) {
        super(statusReason, errorDetails, request);
    }
}
