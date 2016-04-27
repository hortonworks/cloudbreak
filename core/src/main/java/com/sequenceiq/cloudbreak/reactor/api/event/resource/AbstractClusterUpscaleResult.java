package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.ClusterUpscalePayload;

public abstract class AbstractClusterUpscaleResult<R extends AbstractClusterUpscaleRequest> extends ClusterPlatformResult<R> implements ClusterUpscalePayload {

    public AbstractClusterUpscaleResult(R request) {
        super(request);
    }

    public AbstractClusterUpscaleResult(String statusReason, Exception errorDetails, R request) {
        super(statusReason, errorDetails, request);
    }

    @Override
    public String getHostGroupName() {
        return getRequest().getHostGroupName();
    }
}
