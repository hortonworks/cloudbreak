package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum ClusterUpscaleState implements FlowState {
    INIT_STATE,
    UPSCALING_AMBARI_STATE,
    EXECUTING_PRERECIPES_STATE,
    UPSCALING_CLUSTER_STATE,
    EXECUTING_POSTRECIPES_STATE,
    FINALIZE_UPSCALE_STATE,
    CLUSTER_UPSCALE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractAction> clazz;

    ClusterUpscaleState() {
    }

    ClusterUpscaleState(Class<? extends AbstractAction> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<? extends AbstractAction> action() {
        return clazz;
    }
}
