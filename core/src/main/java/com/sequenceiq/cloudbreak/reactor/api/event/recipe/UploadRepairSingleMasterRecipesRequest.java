package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UploadRepairSingleMasterRecipesRequest extends AbstractClusterScaleRequest {
    @JsonCreator
    protected UploadRepairSingleMasterRecipesRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups) {
        super(stackId, hostGroups);
    }
}
