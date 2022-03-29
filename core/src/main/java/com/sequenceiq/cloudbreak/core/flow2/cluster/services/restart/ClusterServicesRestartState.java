package com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum ClusterServicesRestartState implements FlowState {
    INIT_STATE,
    CLUSTER_SERVICE_RESTART_FAILED_STATE,
    CLUSTER_SERVICE_RESTARTING_STATE,
    CLUSTER_SERVICE_RESTART_POLLING_STATE(FillInMemoryStateStoreRestartAction.class),
    CLUSTER_SERVICE_RESTART_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    ClusterServicesRestartState() {

    }

    ClusterServicesRestartState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
