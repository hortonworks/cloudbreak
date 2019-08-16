package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

enum ClusterDownscaleState implements FlowState {
    INIT_STATE,
    COLLECT_CANDIDATES_STATE,
    DECOMMISSION_STATE,
    REMOVE_HOSTS_FROM_ORCHESTRATION_STATE,
    UPDATE_INSTANCE_METADATA_STATE,
    DECOMISSION_FAILED_STATE,
    REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE,
    CLUSTER_DOWNSCALE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
