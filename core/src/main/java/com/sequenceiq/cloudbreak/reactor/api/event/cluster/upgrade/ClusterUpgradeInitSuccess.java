package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeInitSuccess extends StackEvent {
    public ClusterUpgradeInitSuccess(Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return "ClusterUpgradeInitSuccess";
    }
}
