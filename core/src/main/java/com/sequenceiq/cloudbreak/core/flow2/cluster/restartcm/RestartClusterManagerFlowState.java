package com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum RestartClusterManagerFlowState implements FlowState {

    INIT_STATE,
    RESTART_CLUSTER_MANAGER_FLOW_STATE,
    RESTART_CLUSTER_MANAGER_FINISED_STATE,
    FINAL_STATE,
    RESTART_CLUSTER_MANAGER_FAILED_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
