package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterSyncState implements FlowState {
    INIT_STATE,
    CLUSTER_SYNC_FAILED_STATE,

    CLUSTER_SYNC_STATE,
    CLUSTER_SYNC_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
