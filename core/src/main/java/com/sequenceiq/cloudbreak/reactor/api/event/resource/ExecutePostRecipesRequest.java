package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePostRecipesRequest extends AbstractClusterUpscaleRequest {

    public ExecutePostRecipesRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
