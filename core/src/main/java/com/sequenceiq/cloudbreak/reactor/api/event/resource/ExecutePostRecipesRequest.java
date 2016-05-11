package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePostRecipesRequest extends AbstractClusterUpscaleRequest {

    public ExecutePostRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
