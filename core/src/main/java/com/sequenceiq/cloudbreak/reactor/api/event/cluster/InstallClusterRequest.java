package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class InstallClusterRequest extends StackEvent {
    public InstallClusterRequest(Long stackId) {
        super(stackId);
    }
}
