package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum InstanceTerminationState implements FlowState {

    INIT_STATE,
    TERMINATION_FAILED_STATE(InstanceTerminationFailureAction.class),
    TERMINATION_STATE(InstanceTerminationAction.class),
    TERMINATION_FINISHED_STATE(InstanceTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractAction> action;

    InstanceTerminationState() {
    }

    InstanceTerminationState(Class<? extends AbstractAction> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction> action() {
        return action;
    }
}
