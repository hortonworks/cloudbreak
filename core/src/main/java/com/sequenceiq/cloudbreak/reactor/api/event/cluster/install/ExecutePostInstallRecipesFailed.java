package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ExecutePostInstallRecipesFailed extends StackFailureEvent {
    public ExecutePostInstallRecipesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
