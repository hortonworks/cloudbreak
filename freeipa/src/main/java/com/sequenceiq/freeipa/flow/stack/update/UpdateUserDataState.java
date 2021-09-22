package com.sequenceiq.freeipa.flow.stack.update;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.InitializeMDCContextRestartAction;

public enum UpdateUserDataState implements FlowState {
    INIT_STATE,
    UPDATE_USERDATA_STATE,
    UPDATE_USERDATA_ON_PROVIDER_STATE,
    UPDATE_USERDATA_FAILED_STATE,
    UPDATE_USERDATA_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractAction<?, ?, ?, ?>> action;

    UpdateUserDataState(Class<? extends AbstractAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    UpdateUserDataState() {
    }

    @Override
    public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
