package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterResetResult extends ClusterPlatformResult<ClusterResetRequest> {
    public ClusterResetResult(ClusterResetRequest request) {
        super(request);
    }

    public ClusterResetResult(String statusReason, Exception errorDetails, ClusterResetRequest request) {
        super(statusReason, errorDetails, request);
    }
}
