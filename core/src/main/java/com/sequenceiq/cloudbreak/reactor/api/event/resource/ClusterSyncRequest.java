package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterSyncRequest extends ClusterPlatformRequest {
    public ClusterSyncRequest(Long stackId) {
        super(stackId);
    }
}
