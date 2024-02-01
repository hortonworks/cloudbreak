package com.sequenceiq.externalizedcompute.flow.create;

import com.sequenceiq.externalizedcompute.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum ExternalizedComputeClusterCreateState implements FlowState {
    INIT_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_ENV_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_IN_PROGRESS_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    ExternalizedComputeClusterCreateState() {
    }

    ExternalizedComputeClusterCreateState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
