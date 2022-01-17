package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class DecommissionRequest extends AbstractClusterScaleRequest {

    private final Set<Long> privateIds;

    private final ClusterDownscaleDetails details;

    public DecommissionRequest(Long stackId, Set<String> hostGroups, Set<Long> privateIds, ClusterDownscaleDetails details) {
        super(stackId, hostGroups);
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
