package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterResetState implements FlowState<ClusterResetState, ClusterResetEvent> {
    INIT_STATE,
    CLUSTER_RESET_FAILED_STATE,

    CLUSTER_RESET_STATE,
    CLUSTER_RESET_FINISHED_STATE,

    CLUSTER_RESET_START_AMBARI_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
