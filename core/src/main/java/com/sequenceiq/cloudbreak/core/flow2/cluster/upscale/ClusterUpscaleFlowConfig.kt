package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POSTRECIPES_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_POSTRECIPES_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_PRERECIPES_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.EXECUTE_PRERECIPES_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPSCALE_AMBARI_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.UPSCALE_AMBARI_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.values
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.CLUSTER_UPSCALE_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTING_POSTRECIPES_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.EXECUTING_PRERECIPES_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINALIZE_UPSCALE_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_AMBARI_STATE
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState.UPSCALING_CLUSTER_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class ClusterUpscaleFlowConfig : AbstractFlowConfiguration<ClusterUpscaleState, ClusterUpscaleEvent>(ClusterUpscaleState::class.java, ClusterUpscaleEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<ClusterUpscaleState, ClusterUpscaleEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<ClusterUpscaleState, ClusterUpscaleEvent>
        get() = EDGE_CONFIG

    override val events: Array<ClusterUpscaleEvent>
        get() = values()

    override val initEvents: Array<ClusterUpscaleEvent>
        get() = arrayOf(CLUSTER_UPSCALE_TRIGGER_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<ClusterUpscaleState, ClusterUpscaleEvent>().defaultFailureEvent(FAILURE_EVENT).from(INIT_STATE).to(UPSCALING_AMBARI_STATE).event(CLUSTER_UPSCALE_TRIGGER_EVENT).noFailureEvent().from(UPSCALING_AMBARI_STATE).to(EXECUTING_PRERECIPES_STATE).event(UPSCALE_AMBARI_FINISHED_EVENT).failureEvent(UPSCALE_AMBARI_FAILED_EVENT).from(EXECUTING_PRERECIPES_STATE).to(UPSCALING_CLUSTER_STATE).event(EXECUTE_PRERECIPES_FINISHED_EVENT).failureEvent(EXECUTE_PRERECIPES_FAILED_EVENT).from(UPSCALING_CLUSTER_STATE).to(EXECUTING_POSTRECIPES_STATE).event(CLUSTER_UPSCALE_FINISHED_EVENT).failureEvent(CLUSTER_UPSCALE_FAILED_EVENT).from(EXECUTING_POSTRECIPES_STATE).to(FINALIZE_UPSCALE_STATE).event(EXECUTE_POSTRECIPES_FINISHED_EVENT).failureEvent(EXECUTE_POSTRECIPES_FAILED_EVENT).from(FINALIZE_UPSCALE_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE,
                CLUSTER_UPSCALE_FAILED_STATE, FAIL_HANDLED_EVENT)
    }
}
