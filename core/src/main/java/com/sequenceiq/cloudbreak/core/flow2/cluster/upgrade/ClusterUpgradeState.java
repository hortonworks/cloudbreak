package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterUpgradeState implements FlowState {
    INIT_STATE,
    CLUSTER_UPGRADE_FAILED_STATE,

    CLUSTER_UPGRADE_STATE,
    CLUSTER_UPGRADE_FINISHED_STATE,

    FINAL_STATE
}
