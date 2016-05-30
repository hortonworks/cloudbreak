package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStopRequest extends ClusterPlatformRequest {
    public ClusterStopRequest(Long stackId) {
        super(stackId);
    }
}
