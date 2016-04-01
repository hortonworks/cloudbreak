package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum StackTerminationState implements FlowState<StackTerminationState, StackTerminationEvent> {
    INIT_STATE(),
    TERMINATION_FAILED_STATE(StackTerminationFailureAction.class),
    TERMINATION_STATE(StackTerminationAction.class, StackTerminationEvent.TERMINATION_FAILED_EVENT),
    FORCE_TERMINATION_STATE(StackForceTerminationAction.class, StackTerminationEvent.TERMINATION_FAILED_EVENT),
    TERMINATION_FINISHED_STATE(StackTerminationFinishedAction.class, StackTerminationEvent.TERMINATION_FAILED_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private StackTerminationEvent failureEvent;

    StackTerminationState() {
    }

    StackTerminationState(Class<?> action) {
        this.action = action;
    }

    StackTerminationState(Class<?> action, StackTerminationEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public StackTerminationEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public StackTerminationState failureState() {
        return null;
    }
}
