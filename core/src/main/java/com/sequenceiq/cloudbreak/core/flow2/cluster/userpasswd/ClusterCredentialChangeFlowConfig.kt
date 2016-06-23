package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd

import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_FINISHED_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.CLUSTER_CREDENTIALCHANGE_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.CLUSTER_CREDENTIALCHANGE_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.CLUSTER_CREDENTIALCHANGE_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterCredentialChangeFlowConfig : AbstractFlowConfiguration<ClusterCredentialChangeState, ClusterCredentialChangeEvent>(ClusterCredentialChangeState::class.java, ClusterCredentialChangeEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterCredentialChangeState, ClusterCredentialChangeEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterCredentialChangeState, ClusterCredentialChangeEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterCredentialChangeEvent>
        get() = ClusterCredentialChangeEvent.values()

    override val initEvents: Array<ClusterCredentialChangeEvent>
        get() = arrayOf(CLUSTER_CREDENTIALCHANGE_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterCredentialChangeState, ClusterCredentialChangeEvent>().from(INIT_STATE).to(CLUSTER_CREDENTIALCHANGE_STATE).event(CLUSTER_CREDENTIALCHANGE_EVENT).noFailureEvent().from(CLUSTER_CREDENTIALCHANGE_STATE).to(CLUSTER_CREDENTIALCHANGE_FINISHED_STATE).event(CLUSTER_CREDENTIALCHANGE_FINISHED_EVENT).failureEvent(CLUSTER_CREDENTIALCHANGE_FINISHED_FAILURE_EVENT).from(CLUSTER_CREDENTIALCHANGE_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT).build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_CREDENTIALCHANGE_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}
