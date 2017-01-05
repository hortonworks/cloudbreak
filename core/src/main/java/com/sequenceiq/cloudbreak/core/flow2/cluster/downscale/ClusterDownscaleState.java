package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

enum ClusterDownscaleState implements FlowState {
    INIT_STATE,
    COLLECT_CANDIDATES_STATE,
    DECOMMISSION_STATE,
    UPDATE_INSTANCE_METADATA_STATE,
    CLUSTER_DOWNSCALE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
