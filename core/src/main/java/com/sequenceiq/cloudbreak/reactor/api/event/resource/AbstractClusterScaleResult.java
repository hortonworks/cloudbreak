package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;

public abstract class AbstractClusterScaleResult<R extends AbstractClusterScaleRequest> extends ClusterPlatformResult<R> implements HostGroupPayload {

    protected AbstractClusterScaleResult(R request) {
        super(request);
    }

    protected AbstractClusterScaleResult(String statusReason, Exception errorDetails, R request) {
        super(statusReason, errorDetails, request);
    }

    @Override
    public String getHostGroupName() {
        return getRequest().getHostGroupName();
    }
}
