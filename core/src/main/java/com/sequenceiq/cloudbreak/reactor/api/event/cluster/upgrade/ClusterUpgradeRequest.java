package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeRequest extends StackEvent {

    public ClusterUpgradeRequest(Long stackId) {
        super(stackId);
    }

    public static ClusterUpgradeFailedEvent from(StackEvent event, Exception exception) {
        return new ClusterUpgradeFailedEvent(event.getResourceId(), exception);
    }

//    @Override
//    public String selector() {
//        return CLUSTER_UPGRADE_EVENT.event();
//    }
}
