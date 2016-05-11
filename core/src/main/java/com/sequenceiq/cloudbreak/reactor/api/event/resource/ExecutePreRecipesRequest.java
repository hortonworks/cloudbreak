package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePreRecipesRequest extends AbstractClusterUpscaleRequest {

    public ExecutePreRecipesRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
