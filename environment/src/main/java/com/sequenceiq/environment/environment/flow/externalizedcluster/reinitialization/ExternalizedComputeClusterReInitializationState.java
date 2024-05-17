package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ExternalizedComputeClusterReInitializationState implements FlowState {
    INIT_STATE,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE,
    DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
