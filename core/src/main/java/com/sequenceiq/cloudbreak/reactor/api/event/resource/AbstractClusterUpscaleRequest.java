package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.ClusterUpscalePayload;

abstract class AbstractClusterUpscaleRequest extends ClusterPlatformRequest implements ClusterUpscalePayload {
    private final String hostGroupName;

    AbstractClusterUpscaleRequest(Long stackId, String hostGroupName) {
        super(stackId);
        this.hostGroupName = hostGroupName;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }
}
