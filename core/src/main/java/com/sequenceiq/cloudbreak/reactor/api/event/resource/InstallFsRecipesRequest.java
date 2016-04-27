package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallFsRecipesRequest extends AbstractClusterUpscaleRequest {

    public InstallFsRecipesRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
