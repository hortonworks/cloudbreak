package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterCredentialChangeState implements FlowState<ClusterCredentialChangeState, ClusterCredentialChangeEvent> {
    INIT_STATE,
    CLUSTER_CREDENTIALCHANGE_FAILED_STATE,

    CLUSTER_CREDENTIALCHANGE_STATE,
    CLUSTER_CREDENTIALCHANGE_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
