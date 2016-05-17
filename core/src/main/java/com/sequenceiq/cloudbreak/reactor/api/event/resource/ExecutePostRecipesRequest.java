package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePostRecipesRequest extends AbstractClusterScaleRequest {

    public ExecutePostRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
