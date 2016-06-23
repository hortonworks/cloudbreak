package com.sequenceiq.cloudbreak.core.flow2.cluster.reset

import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_FINISHED_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_START_AMBARI_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.CLUSTER_RESET_START_AMBARI_FINISHED_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_START_AMBARI_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.CLUSTER_RESET_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterResetFlowConfig : AbstractFlowConfiguration<ClusterResetState, ClusterResetEvent>(ClusterResetState::class.java, ClusterResetEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterResetState, ClusterResetEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterResetState, ClusterResetEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterResetEvent>
        get() = ClusterResetEvent.values()

    override val initEvents: Array<ClusterResetEvent>
        get() = arrayOf(CLUSTER_RESET_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterResetState, ClusterResetEvent>().from(INIT_STATE).to(CLUSTER_RESET_STATE).event(CLUSTER_RESET_EVENT).noFailureEvent().from(CLUSTER_RESET_STATE).to(CLUSTER_RESET_FINISHED_STATE).event(CLUSTER_RESET_FINISHED_EVENT).failureEvent(CLUSTER_RESET_FINISHED_FAILURE_EVENT).from(CLUSTER_RESET_FINISHED_STATE).to(CLUSTER_RESET_START_AMBARI_FINISHED_STATE).event(CLUSTER_RESET_START_AMBARI_FINISHED_EVENT).failureEvent(CLUSTER_RESET_START_AMBARI_FINISHED_FAILURE_EVENT).from(CLUSTER_RESET_START_AMBARI_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT).build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_RESET_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}
