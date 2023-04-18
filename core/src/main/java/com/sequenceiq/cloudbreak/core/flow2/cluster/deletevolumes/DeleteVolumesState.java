package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DeleteVolumesState implements FlowState {

    INIT_STATE,
    DELETE_VOLUMES_VALIDATION_STATE,
    DELETE_VOLUMES_STATE,
    DELETE_VOLUMES_FINISHED_STATE,
    FINAL_STATE,
    DELETE_VOLUMES_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
