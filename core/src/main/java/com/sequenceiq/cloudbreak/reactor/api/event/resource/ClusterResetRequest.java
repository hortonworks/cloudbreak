package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterResetRequest extends ClusterPlatformRequest {
    public ClusterResetRequest(Long stackId) {
        super(stackId);
    }
}
