package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class CollectDownscaleCandidatesRequest extends AbstractClusterScaleRequest {
    private final Integer scalingAdjustment;

    private final Set<String> hostNames;

    public CollectDownscaleCandidatesRequest(Long stackId, String hostGroupName, Integer scalingAdjustment, Set<String> hostNames) {
        super(stackId, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
        this.hostNames = hostNames;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
