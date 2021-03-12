package com.sequenceiq.freeipa.flow.stack.provision;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.provision.action.CheckImageAction;

public enum StackProvisionState implements FlowState {
    INIT_STATE,
    STACK_CREATION_FAILED_STATE,
    VALIDATION_STATE,
    CREATE_USER_DATA_STATE,
    SETUP_STATE,
    IMAGESETUP_STATE,
    IMAGE_CHECK_STATE(CheckImageAction.class),
    CREATE_CREDENTIAL_STATE,
    START_PROVISIONING_STATE,
    PROVISIONING_FINISHED_STATE,
    COLLECTMETADATA_STATE,
    GET_TLS_INFO_STATE,
    TLS_SETUP_STATE,
    CLUSTERPROXY_REGISTRATION_STATE,
    STACK_CREATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractStackAction<?, ?, ?, ?>> action;

    StackProvisionState() {
    }

    StackProvisionState(Class<? extends AbstractStackAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractStackAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
