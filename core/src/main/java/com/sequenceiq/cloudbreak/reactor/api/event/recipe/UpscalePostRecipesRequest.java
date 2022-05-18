package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscalePostRecipesRequest extends AbstractClusterScaleRequest {

    private final Map<String, Integer> hostGroupWithAdjustment;

    public UpscalePostRecipesRequest(Long stackId, Set<String> hostGroups, Map<String, Integer> hostGroupWithAdjustment) {
        super(stackId, hostGroups);
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }
}
