package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackImageUpdateState implements FlowState {
    INIT_STATE,
    STACK_IMAGE_UPDATE_FAILED_STATE,
    CHECK_IMAGE_VERSIONS_STATE,
    CHECK_PACKAGE_VERSIONS_STATE,
    UPDATE_IMAGE_STATE,
    IMAGE_PREPARE_STATE,
    IMAGE_CHECK_STATE(CheckImageAfterUpdateAction.class),
    SET_IMAGE_STATE,
    STACK_IMAGE_UPDATE_FINISHED,
    FINAL_STATE;

    private Class<? extends AbstractAction<?, ?, ?, ?>> action;

    StackImageUpdateState() {
    }

    StackImageUpdateState(Class<? extends AbstractAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return action;
    }
}
