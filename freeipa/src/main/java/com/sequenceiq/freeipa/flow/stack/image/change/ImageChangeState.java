package com.sequenceiq.freeipa.flow.stack.image.change;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.InitializeMDCContextRestartAction;
import com.sequenceiq.freeipa.flow.stack.image.change.action.CheckImageForChangeAction;

public enum ImageChangeState implements FlowState {
    INIT_STATE,
    CHANGE_IMAGE_STATE,
    PREPARE_IMAGE_STATE,
    CHECK_IMAGE_STATE(CheckImageForChangeAction.class),
    SET_IMAGE_ON_PROVIDER_STATE,
    IMAGE_CHANGE_FAILED_STATE,
    IMAGE_CHANGE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractAction<?, ?, ?, ?>> action;

    ImageChangeState(Class<? extends AbstractAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    ImageChangeState() {
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
