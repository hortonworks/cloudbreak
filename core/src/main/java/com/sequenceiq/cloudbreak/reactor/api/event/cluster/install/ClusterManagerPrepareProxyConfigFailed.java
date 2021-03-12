package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterManagerPrepareProxyConfigFailed extends StackFailureEvent {
    public ClusterManagerPrepareProxyConfigFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}