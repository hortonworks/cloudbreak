package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CheckImageAction;

public enum StackCreationState implements FlowState {
    INIT_STATE,
    STACK_CREATION_FAILED_STATE,
    SETUP_STATE,
    IMAGESETUP_STATE,
    IMAGE_CHECK_STATE(CheckImageAction.class),
    START_PROVISIONING_STATE,
    PROVISIONING_FINISHED_STATE,
    COLLECTMETADATA_STATE,
    TLS_SETUP_STATE,
    STACK_CREATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractAction> action;

    StackCreationState() {
    }

    StackCreationState(Class<? extends AbstractAction> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction> action() {
        return action;
    }
}
