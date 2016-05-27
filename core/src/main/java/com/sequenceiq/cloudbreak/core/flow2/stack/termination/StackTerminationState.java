package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum StackTerminationState implements FlowState {
    INIT_STATE,
    TERMINATION_FAILED_STATE(StackTerminationFailureAction.class),
    TERMINATION_STATE(StackTerminationAction.class),
    FORCE_TERMINATION_STATE(StackForceTerminationAction.class),
    TERMINATION_FINISHED_STATE(StackTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractAction> action;

    StackTerminationState() {
    }

    StackTerminationState(Class<? extends AbstractAction> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction> action() {
        return action;
    }
}
