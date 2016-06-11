package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;

public abstract class AbstractClusterScaleRequest extends ClusterPlatformRequest implements HostGroupPayload {
    private final String hostGroupName;

    public AbstractClusterScaleRequest(Long stackId, String hostGroupName) {
        super(stackId);
        this.hostGroupName = hostGroupName;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }
}
