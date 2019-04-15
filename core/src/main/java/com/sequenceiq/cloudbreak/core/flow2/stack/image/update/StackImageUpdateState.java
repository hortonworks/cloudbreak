package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
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

    private Class<? extends AbstractStackAction<?, ?, ?, ?>> action;

    StackImageUpdateState() {
    }

    StackImageUpdateState(Class<? extends AbstractStackAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractStackAction<?, ?, ?, ?>> action() {
        return action;
    }
}
