package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

enum EphemeralClusterState implements FlowState {
    INIT_STATE,
    EPHEMERAL_CLUSTER_UPDATE_STATE,
    EPHEMERAL_CLUSTER_UPDATE_FINISHED_STATE,
    EPHEMERAL_CLUSTER_UPDATE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
