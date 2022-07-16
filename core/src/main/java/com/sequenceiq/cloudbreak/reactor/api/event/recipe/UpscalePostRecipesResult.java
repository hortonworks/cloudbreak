package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UpscalePostRecipesResult extends AbstractClusterScaleResult<UpscalePostRecipesRequest> implements FlowPayload {

    public UpscalePostRecipesResult(UpscalePostRecipesRequest request) {
        super(request);
    }

    @JsonCreator
    public UpscalePostRecipesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") UpscalePostRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
