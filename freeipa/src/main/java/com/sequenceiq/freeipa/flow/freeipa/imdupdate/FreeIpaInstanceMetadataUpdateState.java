package com.sequenceiq.freeipa.flow.freeipa.imdupdate;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaInstanceMetadataUpdateState implements FlowState {
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
