package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscalePostRecipesResult extends AbstractClusterScaleResult<UpscalePostRecipesRequest> {

    public UpscalePostRecipesResult(UpscalePostRecipesRequest request) {
        super(request);
    }

    public UpscalePostRecipesResult(String statusReason, Exception errorDetails, UpscalePostRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
