package com.sequenceiq.externalizedcompute.flow.delete;

import com.sequenceiq.externalizedcompute.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum ExternalizedComputeClusterDeleteState implements FlowState {
    INIT_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE,
    EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    ExternalizedComputeClusterDeleteState() {
    }

    ExternalizedComputeClusterDeleteState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
