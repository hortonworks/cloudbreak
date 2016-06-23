package com.sequenceiq.cloudbreak.core.flow2.cluster.stop

import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.CLUSTER_STOP_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.CLUSTER_STOPPING_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.CLUSTER_STOP_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterStopFlowConfig : AbstractFlowConfiguration<ClusterStopState, ClusterStopEvent>(ClusterStopState::class.java, ClusterStopEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterStopState, ClusterStopEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterStopState, ClusterStopEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterStopEvent>
        get() = ClusterStopEvent.values()

    override val initEvents: Array<ClusterStopEvent>
        get() = arrayOf(CLUSTER_STOP_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterStopState, ClusterStopEvent>().from(INIT_STATE).to(CLUSTER_STOPPING_STATE).event(CLUSTER_STOP_EVENT).noFailureEvent().from(CLUSTER_STOPPING_STATE).to(ClusterStopState.CLUSTER_STOP_FINISHED_STATE).event(ClusterStopEvent.CLUSTER_STOP_FINISHED_EVENT).failureEvent(ClusterStopEvent.CLUSTER_STOP_FINISHED_FAILURE_EVENT).from(ClusterStopState.CLUSTER_STOP_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).failureEvent(FAILURE_EVENT).build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_STOP_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}

