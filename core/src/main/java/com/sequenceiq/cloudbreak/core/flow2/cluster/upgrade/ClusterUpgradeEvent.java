package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeResult;

public enum ClusterUpgradeEvent implements FlowEvent {
    CLUSTER_UPGRADE_EVENT("CLUSTER_UPGRADE_TRIGGER_EVENT"),
    CLUSTER_UPGRADE_FINISHED_EVENT(EventSelectorUtil.selector(ClusterUpgradeResult.class)),
    CLUSTER_UPGRADE_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(ClusterUpgradeResult.class)),

    CLUSTER_UPGRADE_START_CLUSTER_MANAGER_FINISHED_EVENT(EventSelectorUtil.selector(StartClusterSuccess.class)),
    CLUSTER_UPGRADE_START_CLUSTER_MANAGER_FINISHED_FAILURE_EVENT(EventSelectorUtil.failureSelector(StartClusterFailed.class)),

    FINALIZED_EVENT("CLUSTERUPGRADEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERUPGRADEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERUPGRADEFAILHANDLEDEVENT");

    private final String event;

    ClusterUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
