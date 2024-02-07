package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StackInstanceMetadataUpdateState implements FlowState {
    INIT_STATE,
    STACK_IMDUPDATE_FAILED_STATE,
    STACK_IMDUPDATE_STATE,
    STACK_IMDUPDATE_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
