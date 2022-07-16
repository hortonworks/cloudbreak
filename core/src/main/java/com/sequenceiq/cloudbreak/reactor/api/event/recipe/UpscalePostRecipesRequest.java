package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscalePostRecipesRequest extends AbstractClusterScaleRequest {

    private final Map<String, Integer> hostGroupWithAdjustment;

    @JsonCreator
    public UpscalePostRecipesRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("hostGroupWithAdjustment") Map<String, Integer> hostGroupWithAdjustment) {
        super(stackId, hostGroups);
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }
}
