package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class FinalizeClusterInstallFailed extends StackFailureEvent {
    public FinalizeClusterInstallFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
