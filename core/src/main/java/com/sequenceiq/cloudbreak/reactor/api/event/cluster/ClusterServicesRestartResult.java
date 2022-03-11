package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterServicesRestartResult extends ClusterPlatformResult<ClusterServicesRestartRequest> {

    private final Integer requestId;

    public ClusterServicesRestartResult(ClusterServicesRestartRequest request, int requestId) {
        super(request);
        this.requestId = requestId;
    }

    public ClusterServicesRestartResult(String statusReason, Exception errorDetails, ClusterServicesRestartRequest request) {
        super(statusReason, errorDetails, request);
        requestId = null;
    }

    public Integer getRequestId() {
        return requestId;
    }
}
