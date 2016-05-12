package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum ClusterDownscaleState implements FlowState<ClusterDownscaleState, ClusterDownscaleEvent> {
    INIT_STATE,
    DECOMMISSION_STATE,
    UPDATE_INSTANCE_METADATA_STATE,
    FINALIZE_DOWNSCALE_STATE,
    CLUSTER_DOWNSCALE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
