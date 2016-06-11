package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscalePreRecipesRequest extends AbstractClusterScaleRequest {

    public UpscalePreRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
