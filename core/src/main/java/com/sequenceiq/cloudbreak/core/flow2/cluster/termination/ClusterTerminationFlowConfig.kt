package com.sequenceiq.cloudbreak.core.flow2.cluster.termination


import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent.TERMINATION_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.CLUSTER_TERMINATING_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.CLUSTER_TERMINATION_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.CLUSTER_TERMINATION_FINISH_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterTerminationFlowConfig : AbstractFlowConfiguration<ClusterTerminationState, ClusterTerminationEvent>(ClusterTerminationState::class.java, ClusterTerminationEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterTerminationState, ClusterTerminationEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterTerminationState, ClusterTerminationEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterTerminationEvent>
        get() = ClusterTerminationEvent.values()

    override val initEvents: Array<ClusterTerminationEvent>
        get() = arrayOf(ClusterTerminationEvent.TERMINATION_EVENT)

    companion object {

        private val TRANSITIONS = Transition.Builder<ClusterTerminationState, ClusterTerminationEvent>().defaultFailureEvent(FAILURE_EVENT).from(INIT_STATE).to(CLUSTER_TERMINATING_STATE).event(TERMINATION_EVENT).noFailureEvent().from(CLUSTER_TERMINATING_STATE).to(CLUSTER_TERMINATION_FINISH_STATE).event(TERMINATION_FINISHED_EVENT).failureEvent(TERMINATION_FAILED_EVENT).from(CLUSTER_TERMINATION_FINISH_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, CLUSTER_TERMINATION_FAILED_STATE, FAIL_HANDLED_EVENT)
    }

}
