package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePreRecipesRequest extends AbstractClusterUpscaleRequest {

    public ExecutePreRecipesRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
