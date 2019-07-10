package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.flow.core.FlowState;

public enum StackTerminationState implements FlowState {
    INIT_STATE,
    PRE_TERMINATION_STATE(StackPreTerminationAction.class),
    CLUSTER_PROXY_DEREGISTER_STATE(ClusterProxyDeregisterAction.class),
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
