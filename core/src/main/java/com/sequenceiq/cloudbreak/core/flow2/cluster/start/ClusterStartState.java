package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterStartState implements FlowState {
    INIT_STATE,
    CLUSTER_START_FAILED_STATE,

    CLUSTER_START_UPDATE_PILLAR_CONFIG_STATE,
    UPDATING_DNS_IN_PEM_STATE,
    CLUSTER_STARTING_STATE,
    CLUSTER_START_POLLING_STATE(FillInMemoryStateStoreRestartAction.class),
    CONFIGURE_MANAGEMENT_SERVICES_ON_START_STATE,
    CLUSTER_START_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

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
