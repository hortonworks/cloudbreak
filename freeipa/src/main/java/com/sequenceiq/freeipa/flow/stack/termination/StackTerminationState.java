package com.sequenceiq.freeipa.flow.stack.termination;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.termination.action.DeregisterClusterProxyAction;
import com.sequenceiq.freeipa.flow.stack.termination.action.MachineUserRemoveAction;
import com.sequenceiq.freeipa.flow.stack.termination.action.StackTerminationAction;
import com.sequenceiq.freeipa.flow.stack.termination.action.StackTerminationFailureAction;
import com.sequenceiq.freeipa.flow.stack.termination.action.StackTerminationFinishedAction;

public enum StackTerminationState implements FlowState {
    INIT_STATE,
    DEREGISTER_CLUSTERPROXY_STATE(DeregisterClusterProxyAction.class),
    REMOVE_MACHINE_USER_STATE(MachineUserRemoveAction.class),
    TERMINATION_STATE(StackTerminationAction.class),
    TERMINATION_FAILED_STATE(StackTerminationFailureAction.class),
    TERMINATION_FINISHED_STATE(StackTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractStackAction<?, ?, ?, ?>> action;

    StackTerminationState() {
    }

    StackTerminationState(Class<? extends AbstractStackAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractStackAction<?, ?, ?, ?>> action() {
        return action;
    }
}
