package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterTerminationState implements FlowState {
    INIT_STATE,
    TERMINATION_FAILED_STATE(ClusterTerminationFailureAction.class),
    TERMINATION_STATE(ClusterTerminationAction.class),
    TERMINATION_FINISHED_STATE(ClusterTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractAction> action;

    ClusterTerminationState() {

    }

    ClusterTerminationState(Class<? extends AbstractAction> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction> action() {
        return action;
    }
}
