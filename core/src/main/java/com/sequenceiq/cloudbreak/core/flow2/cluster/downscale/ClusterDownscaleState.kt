package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

internal enum class ClusterDownscaleState : FlowState {
    INIT_STATE,
    DECOMMISSION_STATE,
    UPDATE_INSTANCE_METADATA_STATE,
    FINALIZE_DOWNSCALE_STATE,
    CLUSTER_DOWNSCALE_FAILED_STATE,
    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
