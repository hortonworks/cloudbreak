package com.sequenceiq.cloudbreak.core.flow2.cluster.start

import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_STARTING_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.CLUSTER_START_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterStartFlowConfig : AbstractFlowConfiguration<ClusterStartState, ClusterStartEvent>(ClusterStartState::class.java, ClusterStartEvent::class.java) {

    override val flowTriggerCondition: FlowTriggerCondition
        get() = applicationContext.getBean<ClusterStartFlowTriggerCondition>(ClusterStartFlowTriggerCondition::class.java)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterStartState, ClusterStartEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterStartState, ClusterStartEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterStartEvent>
        get() = ClusterStartEvent.values()

    override val initEvents: Array<ClusterStartEvent>
        get() = arrayOf(CLUSTER_START_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterStartState, ClusterStartEvent>().from(INIT_STATE).to(CLUSTER_STARTING_STATE).event(CLUSTER_START_EVENT).noFailureEvent().from(CLUSTER_STARTING_STATE).to(CLUSTER_START_FINISHED_STATE).event(ClusterStartEvent.CLUSTER_START_FINISHED_EVENT).failureEvent(ClusterStartEvent.CLUSTER_START_FINISHED_FAILURE_EVENT).from(CLUSTER_START_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT).build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_START_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}
