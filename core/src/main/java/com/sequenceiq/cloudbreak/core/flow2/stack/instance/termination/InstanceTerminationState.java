package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum InstanceTerminationState implements FlowState<InstanceTerminationState, InstanceTerminationEvent> {

    INIT_STATE,
    TERMINATION_FAILED_STATE(InstanceTerminationFailureAction.class),
    TERMINATION_STATE(InstanceTerminationAction.class),
    TERMINATION_FINISHED_STATE(InstanceTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<?> action;
    private InstanceTerminationEvent failureEvent;

    InstanceTerminationState() {
    }

    InstanceTerminationState(Class<?> action) {
        this.action = action;
    }


    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public InstanceTerminationState failureState() {
        return null;
    }
}
