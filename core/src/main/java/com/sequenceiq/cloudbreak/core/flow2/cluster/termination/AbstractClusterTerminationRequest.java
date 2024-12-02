package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public abstract class AbstractClusterTerminationRequest extends ClusterPlatformRequest {

    private boolean forced;

    protected AbstractClusterTerminationRequest(Long stackId, boolean forced) {
        super(stackId);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }

}
