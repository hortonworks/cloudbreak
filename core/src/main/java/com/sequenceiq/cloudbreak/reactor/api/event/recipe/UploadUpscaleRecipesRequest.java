package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UploadUpscaleRecipesRequest extends AbstractClusterScaleRequest {

    public UploadUpscaleRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
