package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterTerminationState implements FlowState {
    INIT_STATE,

    CLUSTER_TERMINATION_FAILED_STATE,
    PREPARE_CLUSTER_STATE,
    DEREGISTER_SERVICES_STATE,
    DISABLE_KERBEROS_STATE,
    CLUSTER_TERMINATING_STATE,
    CLUSTER_TERMINATION_FINISH_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
