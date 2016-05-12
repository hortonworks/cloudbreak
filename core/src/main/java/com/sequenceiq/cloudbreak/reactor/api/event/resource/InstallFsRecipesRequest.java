package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallFsRecipesRequest extends AbstractClusterScaleRequest {

    public InstallFsRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
