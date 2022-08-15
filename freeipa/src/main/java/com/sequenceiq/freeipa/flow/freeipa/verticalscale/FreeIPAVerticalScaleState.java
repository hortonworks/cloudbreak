package com.sequenceiq.freeipa.flow.freeipa.verticalscale;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIPAVerticalScaleState implements FlowState {
    INIT_STATE,
    STACK_VERTICALSCALE_FAILED_STATE,
    STACK_VERTICALSCALE_STATE,
    STACK_VERTICALSCALE_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
