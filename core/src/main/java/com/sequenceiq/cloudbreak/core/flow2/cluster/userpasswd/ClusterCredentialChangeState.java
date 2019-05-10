package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import com.sequenceiq.flow.core.FlowState;

public enum ClusterCredentialChangeState implements FlowState {
    INIT_STATE,
    CLUSTER_CREDENTIALCHANGE_FAILED_STATE,

    CLUSTER_CREDENTIALCHANGE_STATE,
    CLUSTER_CREDENTIALCHANGE_FINISHED_STATE,

    FINAL_STATE
}
