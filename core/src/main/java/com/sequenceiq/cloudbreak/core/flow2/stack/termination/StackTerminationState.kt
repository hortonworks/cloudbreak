package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

internal enum class StackTerminationState : FlowState {
    INIT_STATE,
    TERMINATION_FAILED_STATE(StackTerminationFailureAction::class.java),
    TERMINATION_STATE(StackTerminationAction::class.java),
    FORCE_TERMINATION_STATE(StackForceTerminationAction::class.java),
    TERMINATION_FINISHED_STATE(StackTerminationFinishedAction::class.java),
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
