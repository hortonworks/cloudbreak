package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ExecutePostInstallRecipesSuccess extends StackEvent {
    public ExecutePostInstallRecipesSuccess(Long stackId) {
        super(stackId);
    }
}
