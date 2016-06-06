package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterTerminationState implements FlowState {
    INIT_STATE,

    CLUSTER_TERMINATION_FAILED_STATE,
    CLUSTER_TERMINATING_STATE,
    CLUSTER_TERMINATION_FINISH_STATE,

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
