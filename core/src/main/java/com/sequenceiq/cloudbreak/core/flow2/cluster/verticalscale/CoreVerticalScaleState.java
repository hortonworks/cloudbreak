package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum CoreVerticalScaleState implements FlowState {
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
