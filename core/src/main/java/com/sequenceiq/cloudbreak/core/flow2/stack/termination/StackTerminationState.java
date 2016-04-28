package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum StackTerminationState implements FlowState<StackTerminationState, StackTerminationEvent> {
    INIT_STATE(),
    TERMINATION_FAILED_STATE(StackTerminationFailureAction.class),
    TERMINATION_STATE(StackTerminationAction.class),
    FORCE_TERMINATION_STATE(StackForceTerminationAction.class),
    TERMINATION_FINISHED_STATE(StackTerminationFinishedAction.class),
    FINAL_STATE();

    private Class<?> action;

    StackTerminationState() {
    }

    StackTerminationState(Class<?> action) {
        this.action = action;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public StackTerminationState failureState() {
        return null;
    }
}
