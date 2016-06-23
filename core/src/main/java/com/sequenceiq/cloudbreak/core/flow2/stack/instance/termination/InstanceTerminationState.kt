package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

internal enum class InstanceTerminationState : FlowState {

    INIT_STATE,
    TERMINATION_FAILED_STATE(InstanceTerminationFailureAction::class.java),
    TERMINATION_STATE(InstanceTerminationAction::class.java),
    TERMINATION_FINISHED_STATE(InstanceTerminationFinishedAction::class.java),
    FINAL_STATE;

    private val action: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>

    private constructor() {
    }

    private constructor(action: Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>) {
        this.action = action
    }


    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>> {
        return action
    }
}
