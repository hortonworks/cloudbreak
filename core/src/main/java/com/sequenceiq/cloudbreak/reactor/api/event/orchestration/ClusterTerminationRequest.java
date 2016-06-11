package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterTerminationRequest extends ClusterPlatformRequest {
    private final Long clusterId;

    public ClusterTerminationRequest(Long stackId, Long clusterId) {
        super(stackId);
        this.clusterId = clusterId;
    }

    public Long getClusterId() {
        return clusterId;
    }
}
