package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeSuccess extends StackEvent {

    @JsonCreator
    public ClusterUpgradeSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return CLUSTER_UPGRADE_FINISHED_EVENT.event();
    }

}
