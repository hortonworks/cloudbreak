package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.flow.core.FlowState;

public enum ClusterTerminationState implements FlowState {
    INIT_STATE,

    CLUSTER_TERMINATION_FAILED_STATE,
    PREPARE_CLUSTER_STATE,
    DEREGISTER_SERVICES_STATE,
    DISABLE_KERBEROS_STATE,
    CLUSTER_TERMINATING_STATE,
    CLUSTER_TERMINATION_FINISH_STATE,

    FINAL_STATE
}
