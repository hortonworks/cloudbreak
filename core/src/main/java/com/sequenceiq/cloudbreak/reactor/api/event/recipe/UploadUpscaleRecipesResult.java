package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UploadUpscaleRecipesResult extends AbstractClusterScaleResult<UploadUpscaleRecipesRequest> implements FlowPayload {

    public UploadUpscaleRecipesResult(UploadUpscaleRecipesRequest request) {
        super(request);
    }

    @JsonCreator
    public UploadUpscaleRecipesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") UploadUpscaleRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
