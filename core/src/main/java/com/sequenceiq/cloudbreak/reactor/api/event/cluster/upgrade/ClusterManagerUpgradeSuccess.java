package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeSuccess extends StackEvent {

    public ClusterManagerUpgradeSuccess(Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT.event();
    }

}
