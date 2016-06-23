package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

internal enum class ClusterUpscaleState : FlowState {
    INIT_STATE,
    UPSCALING_AMBARI_STATE,
    EXECUTING_PRERECIPES_STATE,
    UPSCALING_CLUSTER_STATE,
    EXECUTING_POSTRECIPES_STATE,
    FINALIZE_UPSCALE_STATE,
    CLUSTER_UPSCALE_FAILED_STATE,
    FINAL_STATE;

    private val clazz: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>

    private constructor() {
    }

    private constructor(clazz: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>) {
        this.clazz = clazz
    }

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>> {
        return clazz
    }
}
