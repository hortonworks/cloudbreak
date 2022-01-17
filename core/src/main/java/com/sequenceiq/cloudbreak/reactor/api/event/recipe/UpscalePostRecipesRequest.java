package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscalePostRecipesRequest extends AbstractClusterScaleRequest {

    public UpscalePostRecipesRequest(Long stackId, Set<String> hostGroups) {
        super(stackId, hostGroups);
    }
}
