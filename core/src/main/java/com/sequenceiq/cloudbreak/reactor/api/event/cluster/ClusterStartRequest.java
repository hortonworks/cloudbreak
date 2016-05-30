package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStartRequest extends ClusterPlatformRequest {
    public ClusterStartRequest(Long stackId) {
        super(stackId);
    }
}
