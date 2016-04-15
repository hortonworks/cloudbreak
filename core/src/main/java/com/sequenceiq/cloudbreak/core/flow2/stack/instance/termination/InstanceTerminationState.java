package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum InstanceTerminationState implements FlowState<InstanceTerminationState, InstanceTerminationEvent> {

    INIT_STATE(),
    TERMINATION_FAILED_STATE(InstanceTerminationFailureAction.class),
    TERMINATION_STATE(InstanceTerminationAction.class, InstanceTerminationEvent.TERMINATION_FAILED_EVENT),
    TERMINATION_FINISHED_STATE(InstanceTerminationFinishedAction.class, InstanceTerminationEvent.TERMINATION_FAILED_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private InstanceTerminationEvent failureEvent;

    InstanceTerminationState() {
    }

    InstanceTerminationState(Class<?> action) {
        this.action = action;
    }

    InstanceTerminationState(Class<?> action, InstanceTerminationEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public InstanceTerminationEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public InstanceTerminationState failureState() {
        return null;
    }

    @Override
    public void setFailureEvent(InstanceTerminationEvent failureEvent) {
        this.failureEvent = failureEvent;
    }
}
