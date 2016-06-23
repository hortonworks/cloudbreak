package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class StackDownscaleState : FlowState {
    INIT_STATE,
    DOWNSCALE_FAILED_STATE,
    DOWNSCALE_STATE,
    DOWNSCALE_FINISHED_STATE,
    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }

}
