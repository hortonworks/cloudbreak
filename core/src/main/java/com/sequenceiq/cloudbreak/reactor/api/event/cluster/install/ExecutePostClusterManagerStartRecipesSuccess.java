package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ExecutePostClusterManagerStartRecipesSuccess extends StackEvent {
    public ExecutePostClusterManagerStartRecipesSuccess(Long stackId) {
        super(stackId);
    }
}
