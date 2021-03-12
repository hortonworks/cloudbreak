package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerPrepareProxyConfigSuccess extends StackEvent {
    public ClusterManagerPrepareProxyConfigSuccess(Long stackId) {
        super(stackId);
    }
}
