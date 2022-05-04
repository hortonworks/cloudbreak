package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterProxyReRegistrationResult extends ClusterPlatformResult<ClusterProxyReRegistrationRequest> {
    public ClusterProxyReRegistrationResult(ClusterProxyReRegistrationRequest request) {
        super(request);
    }

    public ClusterProxyReRegistrationResult(String statusReason, Exception errorDetails, ClusterProxyReRegistrationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
