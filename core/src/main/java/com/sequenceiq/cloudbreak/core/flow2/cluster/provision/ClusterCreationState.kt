package com.sequenceiq.cloudbreak.core.flow2.cluster.provision

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterCreationState private constructor() : FlowState {
    INIT_STATE,
    CLUSTER_CREATION_FAILED_STATE,
    STARTING_AMBARI_SERVICES_STATE,
    STARTING_AMBARI_STATE,
    INSTALLING_CLUSTER_STATE,
    CLUSTER_CREATION_FINISHED_STATE,
    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
