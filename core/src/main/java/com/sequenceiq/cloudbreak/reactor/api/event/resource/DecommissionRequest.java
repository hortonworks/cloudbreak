package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class DecommissionRequest extends AbstractClusterScaleRequest {

    private final Set<Long> privateIds;

    public DecommissionRequest(Long stackId, String hostGroupName, Set<Long> privateIds) {
        super(stackId, hostGroupName);
        this.privateIds = privateIds;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }
}
