package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscalePostRecipesRequest extends AbstractClusterScaleRequest {

    public UpscalePostRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
