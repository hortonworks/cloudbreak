package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStartPillarConfigUpdateRequest extends ClusterPlatformRequest {
    public ClusterStartPillarConfigUpdateRequest(Long stackId) {
        super(stackId);
    }
}