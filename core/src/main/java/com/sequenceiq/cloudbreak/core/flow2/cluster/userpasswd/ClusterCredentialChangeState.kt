package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowState

enum class ClusterCredentialChangeState : FlowState {
    INIT_STATE,
    CLUSTER_CREDENTIALCHANGE_FAILED_STATE,

    CLUSTER_CREDENTIALCHANGE_STATE,
    CLUSTER_CREDENTIALCHANGE_FINISHED_STATE,

    FINAL_STATE;

    override fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>? {
        return null
    }
}
