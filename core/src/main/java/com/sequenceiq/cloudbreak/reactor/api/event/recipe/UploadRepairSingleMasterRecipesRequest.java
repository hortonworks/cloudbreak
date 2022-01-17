package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UploadRepairSingleMasterRecipesRequest extends AbstractClusterScaleRequest {
    protected UploadRepairSingleMasterRecipesRequest(Long stackId, Set<String> hostGroups) {
        super(stackId, hostGroups);
    }
}
