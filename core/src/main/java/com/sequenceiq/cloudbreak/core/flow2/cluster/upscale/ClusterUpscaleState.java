package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

enum ClusterUpscaleState implements FlowState {
    INIT_STATE,
    UPLOAD_UPSCALE_RECIPES_STATE,
    UPSCALING_AMBARI_STATE,
    UPSCALING_CLUSTER_STATE,
    EXECUTING_POSTRECIPES_STATE,
    FINALIZE_UPSCALE_STATE,
    CLUSTER_UPSCALE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
