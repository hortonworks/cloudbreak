package com.sequenceiq.cloudbreak.core.flow2.stack.upscale

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class StackUpscaleState : FlowState {
    INIT_STATE,
    UPSCALE_FAILED_STATE,
    ADD_INSTANCES_STATE,
    ADD_INSTANCES_FINISHED_STATE,
    EXTEND_METADATA_STATE,
    EXTEND_METADATA_FINISHED_STATE,
    BOOTSTRAP_NEW_NODES_STATE,
    EXTEND_HOST_METADATA_STATE,
    EXTEND_HOST_METADATA_FINISHED_STATE,
    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
