package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public abstract class AbstractClusterScaleResult<R extends AbstractClusterScaleRequest> extends ClusterPlatformResult<R> {

    protected AbstractClusterScaleResult(R request) {
        super(request);
    }

    protected AbstractClusterScaleResult(String statusReason, Exception errorDetails, R request) {
        super(statusReason, errorDetails, request);
    }

    protected AbstractClusterScaleResult(EventStatus status, String statusReason, Exception errorDetails, R request) {
        super(status, statusReason, errorDetails, request);
    }

    public Set<String> getHostGroupNames() {
        return getRequest().getHostGroupNames();
    }
}
