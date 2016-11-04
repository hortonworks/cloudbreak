package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterUpgradeRequest extends ClusterPlatformRequest {
    public ClusterUpgradeRequest(Long stackId) {
        super(stackId);
    }
}
