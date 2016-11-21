package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.core.flow2.restart.DefaultRestartAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterStartState implements FlowState {
    INIT_STATE,
    CLUSTER_START_FAILED_STATE,

    CLUSTER_STARTING_STATE,
    CLUSTER_START_POLLING_STATE(FillInMemoryStateStoreRestartAction.class),
    CLUSTER_START_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = DefaultRestartAction.class;

    ClusterStartState() {

    }

    ClusterStartState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
