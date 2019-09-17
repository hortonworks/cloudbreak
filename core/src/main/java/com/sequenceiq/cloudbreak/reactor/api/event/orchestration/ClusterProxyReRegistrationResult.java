package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class ClusterProxyReRegistrationResult extends AbstractClusterScaleResult<ClusterProxyReRegistrationRequest> {
    public ClusterProxyReRegistrationResult(ClusterProxyReRegistrationRequest request) {
        super(request);
    }

    public ClusterProxyReRegistrationResult(String statusReason, Exception errorDetails, ClusterProxyReRegistrationRequest request) {
        super(statusReason, errorDetails, request);
    }
}
