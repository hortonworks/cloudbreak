package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscalePreRecipesResult extends AbstractClusterScaleResult<UpscalePreRecipesRequest> {

    public UpscalePreRecipesResult(UpscalePreRecipesRequest request) {
        super(request);
    }

    public UpscalePreRecipesResult(String statusReason, Exception errorDetails, UpscalePreRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
