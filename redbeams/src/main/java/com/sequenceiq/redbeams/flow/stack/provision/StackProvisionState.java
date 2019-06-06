package com.sequenceiq.redbeams.flow.stack.provision;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.redbeams.flow.stack.AbstractRedbeamsAction;

public enum StackProvisionState implements FlowState {
    INIT_STATE,
    STACK_CREATION_FAILED_STATE,
    VALIDATION_STATE,
    SETUP_STATE,
    IMAGESETUP_STATE,
    CREATE_CREDENTIAL_STATE,
    START_PROVISIONING_STATE,
    PROVISIONING_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractRedbeamsAction<?, ?, ?, ?>> action;

    StackProvisionState() {
    }

    StackProvisionState(Class<? extends AbstractRedbeamsAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractRedbeamsAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return null;
    }
}
