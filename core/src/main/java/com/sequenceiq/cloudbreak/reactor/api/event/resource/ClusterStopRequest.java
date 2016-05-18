package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterStopRequest extends ClusterPlatformRequest {
    public ClusterStopRequest(Long stackId) {
        super(stackId);
    }
}
