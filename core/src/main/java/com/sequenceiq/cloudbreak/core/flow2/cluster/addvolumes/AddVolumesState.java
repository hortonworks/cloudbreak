package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum AddVolumesState implements FlowState {
    INIT_STATE,
    ADD_VOLUMES_VALIDATE_STATE,
    ADD_VOLUMES_STATE,
    ATTACH_VOLUMES_STATE,
    ADD_VOLUMES_ORCHESTRATION_STATE,
    ADD_VOLUMES_CM_CONFIGURATION_STATE,
    ADD_VOLUMES_FINISHED_STATE,
    FINAL_STATE,
    ADD_VOLUMES_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}