package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeRequest extends StackEvent {

    public ClusterManagerUpgradeRequest(Long stackId) {
        super(stackId);
    }

    public static ClusterUpgradeFailedEvent from(StackEvent event, Exception exception) {
        return new ClusterUpgradeFailedEvent(event.getResourceId(), exception);
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }
}
