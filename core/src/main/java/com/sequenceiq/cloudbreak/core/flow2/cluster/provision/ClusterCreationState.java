package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterCreationState implements FlowState {
    INIT_STATE,
    CLUSTER_CREATION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    STARTING_AMBARI_SERVICES_STATE,
    STARTING_AMBARI_STATE,
    INSTALLING_CLUSTER_STATE,
    CLUSTER_CREATION_FINISHED_STATE,
    FINAL_STATE
}
