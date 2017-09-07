package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public abstract class AbstractClusterBootstrapResult<R extends AbstractClusterBootstrapRequest> extends ClusterPlatformResult<R> {
    protected AbstractClusterBootstrapResult(R request) {
        super(request);
    }

    protected AbstractClusterBootstrapResult(String statusReason, Exception errorDetails, R request) {
        super(statusReason, errorDetails, request);
    }
}
