package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterResetState implements FlowState {
    INIT_STATE,
    CLUSTER_RESET_FAILED_STATE,

    CLUSTER_RESET_STATE,
    CLUSTER_RESET_FINISHED_STATE,

    CLUSTER_RESET_START_AMBARI_FINISHED_STATE,

    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
