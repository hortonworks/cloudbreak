package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import com.sequenceiq.flow.core.FlowEvent;

public enum ClusterUpgradeEvent implements FlowEvent {
    CLUSTER_MANAGER_UPGRADE_EVENT("CLUSTER_MANAGER_UPGRADE_EVENT"),
    CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT("CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT"),

    CLUSTER_UPGRADE_FINISHED_EVENT("CLUSTER_UPGRADE_FINISHED_EVENT"),

    CLUSTER_UPGRADE_FAILED_EVENT("CLUSTER_UPGRADE_FAILED_EVENT"),
    CLUSTER_UPGRADE_FAIL_HANDLED_EVENT("CLUSTER_UPGRADE_FAIL_HANDLED_EVENT"),
    CLUSTER_UPGRADE_FINALIZED_EVENT("CLUSTER_UPGRADE_FINALIZED_EVENT");

    private final String event;

    ClusterUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
