package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterTerminationState implements FlowState<ClusterTerminationState, ClusterTerminationEvent> {
    INIT_STATE(),
    TERMINATION_FAILED_STATE(ClusterTerminationFailureAction.class),
    TERMINATION_STATE(ClusterTerminationAction.class, ClusterTerminationEvent.TERMINATION_FAILED_EVENT),
    TERMINATION_FINISHED_STATE(ClusterTerminationFinishedAction.class, ClusterTerminationEvent.TERMINATION_FAILED_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private ClusterTerminationEvent failureEvent;
    private ClusterTerminationState failureState;

    ClusterTerminationState() {

    }

    ClusterTerminationState(Class<?> action) {
        this.action = action;
    }

    ClusterTerminationState(Class<?> action, ClusterTerminationEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    ClusterTerminationState(Class<?> action, ClusterTerminationEvent failureEvent, ClusterTerminationState failureState) {
        this.action = action;
        this.failureEvent = failureEvent;
        this.failureState = failureState;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public ClusterTerminationState failureState() {
        return failureState;
    }

    @Override
    public ClusterTerminationEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public void setFailureEvent(ClusterTerminationEvent failureEvent) {
        this.failureEvent = failureEvent;
    }
}
