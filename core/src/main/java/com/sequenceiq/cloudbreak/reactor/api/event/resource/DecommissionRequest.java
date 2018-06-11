package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class DecommissionRequest extends AbstractClusterScaleRequest {

    private final Set<Long> privateIds;

    private final ClusterDownscaleDetails details;

    public DecommissionRequest(Long stackId, String hostGroupName, Set<Long> privateIds, ClusterDownscaleDetails details) {
        super(stackId, hostGroupName);
        this.privateIds = privateIds;
        this.details = details;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }
}
