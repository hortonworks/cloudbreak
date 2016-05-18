package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterStartState implements FlowState<ClusterStartState, ClusterStartEvent> {
    INIT_STATE,
    CLUSTER_START_FAILED_STATE,

    CLUSTER_START_STATE,
    CLUSTER_START_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
