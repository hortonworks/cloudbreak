package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UploadUpscaleRecipesResult extends AbstractClusterScaleResult<UploadUpscaleRecipesRequest> {

    public UploadUpscaleRecipesResult(UploadUpscaleRecipesRequest request) {
        super(request);
    }

    public UploadUpscaleRecipesResult(String statusReason, Exception errorDetails, UploadUpscaleRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
