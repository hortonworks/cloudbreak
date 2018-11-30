package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class UploadRepairSingleMasterRecipesResult extends AbstractClusterScaleResult<UploadRepairSingleMasterRecipesRequest> {

    public UploadRepairSingleMasterRecipesResult(UploadRepairSingleMasterRecipesRequest request) {
        super(request);
    }

    public UploadRepairSingleMasterRecipesResult(String statusReason, Exception errorDetails, UploadRepairSingleMasterRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
