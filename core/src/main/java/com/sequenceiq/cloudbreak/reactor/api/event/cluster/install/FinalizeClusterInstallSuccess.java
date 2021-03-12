package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class FinalizeClusterInstallSuccess extends StackEvent {
    public FinalizeClusterInstallSuccess(Long stackId) {
        super(stackId);
    }
}
