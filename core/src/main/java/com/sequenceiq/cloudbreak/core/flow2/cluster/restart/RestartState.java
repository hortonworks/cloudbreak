package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum RestartState implements FlowState {
    INIT_STATE,
    RESTART_FAILED_STATE,
    RESTART_STATE,
    RESTART_FINISHED_STATE,
    FINAL_STATE;
    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
