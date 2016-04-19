package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CheckImageAction;

public enum StackCreationState implements FlowState<StackCreationState, StackCreationEvent> {
    INIT_STATE,
    STACK_CREATION_FAILED_STATE,
    SETUP_STATE,
    IMAGESETUP_STATE,
    IMAGE_CHECK_STATE(CheckImageAction.class),
    START_PROVISIONING_STATE,
    PROVISIONING_FINISHED_STATE,
    COLLECTMETADATA_STATE,
    TLS_SETUP_STATE,
    FINAL_STATE;

    private Class<?> action;
    private StackCreationEvent failureEvent;
    private StackCreationState failureState;

    StackCreationState() {
    }

    StackCreationState(Class<?> action) {
        this.action = action;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public StackCreationEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public StackCreationState failureState() {
        return failureState;
    }

    @Override
    public void setFailureEvent(StackCreationEvent failureEvent) {
        this.failureEvent = failureEvent;
    }
}
