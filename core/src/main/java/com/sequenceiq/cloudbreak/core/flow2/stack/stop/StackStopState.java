package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum StackStopState implements FlowState {
    INIT_STATE,
    STOP_FAILED_STATE,
    STOP_STATE(StackStopRestartAction.class),
    STOP_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = InitializeMDCContextRestartAction.class;

    StackStopState() {

    }

    StackStopState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
