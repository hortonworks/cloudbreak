package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeRequest extends StackEvent {

    public ClusterManagerUpgradeRequest(Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }
}
