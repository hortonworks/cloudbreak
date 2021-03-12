package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerRefreshParcelRequest extends StackEvent {
    public ClusterManagerRefreshParcelRequest(Long stackId) {
        super(stackId);
    }
}
