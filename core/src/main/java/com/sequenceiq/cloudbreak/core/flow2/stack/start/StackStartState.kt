package com.sequenceiq.cloudbreak.core.flow2.stack.start

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class StackStartState : FlowState {
    INIT_STATE,
    START_FAILED_STATE,
    START_STATE,
    COLLECTING_METADATA,
    START_FINISHED_STATE,
    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
