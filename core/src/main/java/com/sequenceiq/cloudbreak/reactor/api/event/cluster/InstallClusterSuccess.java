package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class InstallClusterSuccess extends StackEvent {
    public InstallClusterSuccess(Long stackId) {
        super(stackId);
    }
}
