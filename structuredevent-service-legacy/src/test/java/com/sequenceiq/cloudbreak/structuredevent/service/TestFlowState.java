package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum TestFlowState implements FlowState {
    INIT_STATE,
    TEMP_STATE,
    NOT_THE_LATEST_FAILED_STATE,
    FAILED_STATE,
    FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return null;
    }
}
