package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_EVENT;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeSuccess extends StackEvent {

    public ClusterUpgradeSuccess(Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return CLUSTER_UPGRADE_FINISHED_EVENT.event();
    }

}
