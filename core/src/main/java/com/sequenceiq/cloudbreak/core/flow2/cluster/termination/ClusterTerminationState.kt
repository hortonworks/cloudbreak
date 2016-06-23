package com.sequenceiq.cloudbreak.core.flow2.cluster.termination

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterTerminationState : FlowState {
    INIT_STATE,

    CLUSTER_TERMINATION_FAILED_STATE,
    CLUSTER_TERMINATING_STATE,
    CLUSTER_TERMINATION_FINISH_STATE,

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
