package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallRecipesRequest extends AbstractClusterScaleRequest {

    public InstallRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
