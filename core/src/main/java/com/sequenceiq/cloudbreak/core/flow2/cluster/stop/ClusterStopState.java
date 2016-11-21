package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import com.sequenceiq.cloudbreak.core.flow2.restart.DefaultRestartAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterStopState implements FlowState {
    INIT_STATE,
    CLUSTER_STOP_FAILED_STATE,

    CLUSTER_STOPPING_STATE(FillInMemoryStateStoreRestartAction.class),
    CLUSTER_STOP_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = DefaultRestartAction.class;

    ClusterStopState() {

    }

    ClusterStopState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
