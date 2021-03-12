package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerRefreshParcelSuccess extends StackEvent {
    public ClusterManagerRefreshParcelSuccess(Long stackId) {
        super(stackId);
    }
}
