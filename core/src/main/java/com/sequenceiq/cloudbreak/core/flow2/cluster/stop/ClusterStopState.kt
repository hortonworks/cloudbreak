package com.sequenceiq.cloudbreak.core.flow2.cluster.stop

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterStopState : FlowState {
    INIT_STATE,
    CLUSTER_STOP_FAILED_STATE,

    CLUSTER_STOPPING_STATE,
    CLUSTER_STOP_FINISHED_STATE,

    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
