package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class PrepareClusterTerminationRequest extends ClusterPlatformRequest {

    public PrepareClusterTerminationRequest(Long stackId) {
        super(stackId);
    }
}
