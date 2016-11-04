package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeResult;

public enum ClusterUpgradeEvent implements FlowEvent {
    CLUSTER_UPGRADE_EVENT(FlowTriggers.CLUSTER_UPGRADE_TRIGGER_EVENT),
    CLUSTER_UPGRADE_FINISHED_EVENT(EventSelectorUtil.selector(ClusterUpgradeResult.class)),
    CLUSTER_UPGRADE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterUpgradeResult.class)),

    CLUSTER_UPGRADE_START_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariSuccess.class)),
    CLUSTER_UPGRADE_START_AMBARI_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(StartAmbariFailed.class)),

    FINALIZED_EVENT("CLUSTERUPGRADEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERUPGRADEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERUPGRADEFAILHANDLEDEVENT");

    private String stringRepresentation;

    ClusterUpgradeEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
