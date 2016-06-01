package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterStartState implements FlowState {
    INIT_STATE,
    CLUSTER_START_FAILED_STATE,

    CLUSTER_STARTING_STATE,
    CLUSTER_START_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends AbstractAction> action() {
        return null;
    }
}
