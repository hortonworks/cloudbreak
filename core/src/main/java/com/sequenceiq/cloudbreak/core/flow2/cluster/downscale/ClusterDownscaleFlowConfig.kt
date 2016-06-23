package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.UPDATE_METADATA_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.UPDATE_METADATA_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.CLUSTER_DOWNSCALE_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.DECOMMISSION_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.FINALIZE_DOWNSCALE_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState.UPDATE_INSTANCE_METADATA_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterDownscaleFlowConfig : AbstractFlowConfiguration<ClusterDownscaleState, ClusterDownscaleEvent>(ClusterDownscaleState::class.java, ClusterDownscaleEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterDownscaleState, ClusterDownscaleEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterDownscaleState, ClusterDownscaleEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterDownscaleEvent>
        get() = ClusterDownscaleEvent.values()

    override val initEvents: Array<ClusterDownscaleEvent>
        get() = arrayOf(DECOMMISSION_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterDownscaleState, ClusterDownscaleEvent>().defaultFailureEvent(FAILURE_EVENT).from(INIT_STATE).to(DECOMMISSION_STATE).event(DECOMMISSION_EVENT).noFailureEvent().from(DECOMMISSION_STATE).to(UPDATE_INSTANCE_METADATA_STATE).event(DECOMMISSION_FINISHED_EVENT).failureEvent(DECOMMISSION_FAILED_EVENT).from(UPDATE_INSTANCE_METADATA_STATE).to(FINALIZE_DOWNSCALE_STATE).event(UPDATE_METADATA_FINISHED_EVENT).failureEvent(UPDATE_METADATA_FAILED_EVENT).from(FINALIZE_DOWNSCALE_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_DOWNSCALE_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}
