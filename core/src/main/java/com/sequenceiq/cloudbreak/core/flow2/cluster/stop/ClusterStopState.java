package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ClusterStopState implements FlowState {
    INIT_STATE,
    CLUSTER_STOP_FAILED_STATE,

    CLUSTER_STOPPING_STATE,
    CLUSTER_STOP_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends AbstractAction> action() {
        return null;
    }
}
