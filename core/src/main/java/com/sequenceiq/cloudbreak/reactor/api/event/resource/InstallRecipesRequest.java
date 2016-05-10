package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallRecipesRequest extends AbstractClusterUpscaleRequest {

    public InstallRecipesRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
