package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ExecutePostInstallRecipesRequest extends StackEvent {
    public ExecutePostInstallRecipesRequest(Long stackId) {
        super(stackId);
    }
}
