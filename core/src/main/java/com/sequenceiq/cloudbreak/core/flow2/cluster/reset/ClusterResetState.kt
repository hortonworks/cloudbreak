package com.sequenceiq.cloudbreak.core.flow2.cluster.reset

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterResetState : FlowState {
    INIT_STATE,
    CLUSTER_RESET_FAILED_STATE,

    CLUSTER_RESET_STATE,
    CLUSTER_RESET_FINISHED_STATE,

    CLUSTER_RESET_START_AMBARI_FINISHED_STATE,

    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
