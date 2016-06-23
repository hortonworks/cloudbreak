package com.sequenceiq.cloudbreak.core.flow2.cluster.sync

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterSyncState : FlowState {
    INIT_STATE,
    CLUSTER_SYNC_FAILED_STATE,

    CLUSTER_SYNC_STATE,
    CLUSTER_SYNC_FINISHED_STATE,

    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
