package com.sequenceiq.cloudbreak.core.flow2.cluster.start

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterStartState : FlowState {
    INIT_STATE,
    CLUSTER_START_FAILED_STATE,

    CLUSTER_STARTING_STATE,
    CLUSTER_START_FINISHED_STATE,

    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
