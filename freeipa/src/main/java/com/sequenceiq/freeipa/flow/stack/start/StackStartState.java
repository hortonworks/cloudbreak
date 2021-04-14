package com.sequenceiq.freeipa.flow.stack.start;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum StackStartState implements FlowState {
    INIT_STATE,
    START_FAILED_STATE,
    START_STATE,
    COLLECTING_METADATA,
    START_SAVE_METADATA_STATE,
    START_WAIT_UNTIL_AVAILABLE_STATE,
    START_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
