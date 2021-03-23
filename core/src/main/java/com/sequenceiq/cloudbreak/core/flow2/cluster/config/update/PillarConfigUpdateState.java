package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum PillarConfigUpdateState implements FlowState {
    INIT_STATE,
    PILLAR_CONFIG_UPDATE_START_STATE,
    PILLAR_CONFIG_UPDATE_FINISHED_STATE,
    FINAL_STATE,
    PILLAR_CONFIG_UPDATE_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}