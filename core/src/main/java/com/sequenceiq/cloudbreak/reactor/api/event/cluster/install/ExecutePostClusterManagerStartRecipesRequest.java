package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ExecutePostClusterManagerStartRecipesRequest extends StackEvent {
    public ExecutePostClusterManagerStartRecipesRequest(Long stackId) {
        super(stackId);
    }
}
