package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_EVENT;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeSuccess extends StackEvent {

    public ClusterUpgradeSuccess(Long stackId) {
        super(stackId);
    }

    public static ClusterUpgradeFailedEvent from(StackEvent event, Exception exception) {
        return new ClusterUpgradeFailedEvent(event.getResourceId(), exception);
    }

    @Override
    public String selector() {
        return CLUSTER_UPGRADE_FINISHED_EVENT.event();
    }

}
