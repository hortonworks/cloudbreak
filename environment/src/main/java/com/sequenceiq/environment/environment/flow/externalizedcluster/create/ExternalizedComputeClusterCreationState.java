package com.sequenceiq.environment.environment.flow.externalizedcluster.create;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ExternalizedComputeClusterCreationState implements FlowState {
    INIT_STATE,
    DEFAULT_COMPUTE_CLUSTER_CREATION_START_STATE,
    DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_STATE,
    DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
