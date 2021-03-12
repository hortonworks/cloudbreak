package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class FinalizeClusterInstallRequest extends StackEvent {
    public FinalizeClusterInstallRequest(Long stackId) {
        super(stackId);
    }
}
