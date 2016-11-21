package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterStartResult extends ClusterPlatformResult<ClusterStartRequest> {

    private final Integer requestId;

    public ClusterStartResult(ClusterStartRequest request, int requestId) {
        super(request);
        this.requestId = requestId;
    }

    public ClusterStartResult(String statusReason, Exception errorDetails, ClusterStartRequest request) {
        super(statusReason, errorDetails, request);
        this.requestId = null;
    }

    public Integer getRequestId() {
        return requestId;
    }
}
