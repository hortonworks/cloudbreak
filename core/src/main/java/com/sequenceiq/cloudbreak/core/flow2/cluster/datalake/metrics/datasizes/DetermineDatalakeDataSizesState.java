package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DetermineDatalakeDataSizesState implements FlowState {
    INIT_STATE,
    DETERMINE_DATALAKE_DATA_SIZES_SALT_UPDATE_STATE,
    DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS_STATE,
    DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_STATE,
    DETERMINE_DATALAKE_DATA_SIZES_FAILURE_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DetermineDatalakeDataSizesState() {
    }

    DetermineDatalakeDataSizesState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
