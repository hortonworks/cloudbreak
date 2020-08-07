package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeInitRequest extends StackEvent {
    public ClusterUpgradeInitRequest(Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return "ClusterUpgradeInitRequest";
    }
}
