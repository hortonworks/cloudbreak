package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterManagerConfigureKerberosFailed extends StackFailureEvent {
    public ClusterManagerConfigureKerberosFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
