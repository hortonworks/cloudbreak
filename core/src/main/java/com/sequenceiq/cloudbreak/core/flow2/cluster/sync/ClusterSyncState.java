package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterSyncState implements FlowState<ClusterSyncState, ClusterSyncEvent> {
    INIT_STATE,
    CLUSTER_SYNC_FAILED_STATE,

    CLUSTER_SYNC_STATE,
    CLUSTER_SYNC_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
