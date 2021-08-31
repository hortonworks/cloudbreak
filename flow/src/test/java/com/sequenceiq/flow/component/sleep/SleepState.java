package com.sequenceiq.flow.component.sleep;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SleepState implements FlowState {
    INIT_STATE,
    SLEEP_STARTED_STATE,
    SLEEP_FINISHED_STATE,
    SLEEP_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }
}