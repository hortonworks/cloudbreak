package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterTerminationState implements FlowState<ClusterTerminationState, ClusterTerminationEvent> {
    INIT_STATE,
    TERMINATION_FAILED_STATE(ClusterTerminationFailureAction.class),
    TERMINATION_STATE(ClusterTerminationAction.class),
    TERMINATION_FINISHED_STATE(ClusterTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<?> action;

    ClusterTerminationState() {

    }

    ClusterTerminationState(Class<?> action) {
        this.action = action;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public ClusterTerminationState failureState() {
        return null;
    }
}
