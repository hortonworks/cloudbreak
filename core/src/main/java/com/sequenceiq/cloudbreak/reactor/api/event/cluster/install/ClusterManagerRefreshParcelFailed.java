package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterManagerRefreshParcelFailed extends StackFailureEvent {
    public ClusterManagerRefreshParcelFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
