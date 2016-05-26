package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterCreationState implements FlowState<ClusterCreationState, ClusterCreationEvent> {
    INIT_STATE,
    CLUSTER_CREATION_FAILED_STATE,
    STARTING_AMBARI_SERVICES_STATE,
    STARTING_AMBARI_STATE,
    INSTALLING_CLUSTER_STATE,
    CLUSTER_CREATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<?> action;

    ClusterCreationState() {
    }

    ClusterCreationState(Class<?> action) {
        this.action = action;
    }

    @Override
    public Class<?> action() {
        return action;
    }
}
