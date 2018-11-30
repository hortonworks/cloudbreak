package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UploadRepairSingleMasterRecipesRequest extends AbstractClusterScaleRequest {
    protected UploadRepairSingleMasterRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
