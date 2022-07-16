package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UploadRepairSingleMasterRecipesResult extends AbstractClusterScaleResult<UploadRepairSingleMasterRecipesRequest> implements FlowPayload {

    public UploadRepairSingleMasterRecipesResult(UploadRepairSingleMasterRecipesRequest request) {
        super(request);
    }

    @JsonCreator
    public UploadRepairSingleMasterRecipesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") UploadRepairSingleMasterRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
