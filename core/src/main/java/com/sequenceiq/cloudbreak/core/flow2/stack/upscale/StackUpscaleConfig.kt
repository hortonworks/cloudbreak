package com.sequenceiq.cloudbreak.core.flow2.stack.upscale

import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_FINISHED_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.EXTEND_METADATA_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.ADD_INSTANCES_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.ADD_INSTANCES_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.BOOTSTRAP_NEW_NODES_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_HOST_METADATA_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_HOST_METADATA_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_METADATA_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.EXTEND_METADATA_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState.UPSCALE_FAILED_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class StackUpscaleConfig : AbstractFlowConfiguration<StackUpscaleState, StackUpscaleEvent>(StackUpscaleState::class.java, StackUpscaleEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<StackUpscaleState, StackUpscaleEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<StackUpscaleState, StackUpscaleEvent>
        get() = EDGE_CONFIG

    override val events: Array<StackUpscaleEvent>
        get() = StackUpscaleEvent.values()

    override val initEvents: Array<StackUpscaleEvent>
        get() = arrayOf(StackUpscaleEvent.ADD_INSTANCES_EVENT)

    companion object {
        private val TRANSITIONS = Transition.Builder<StackUpscaleState, StackUpscaleEvent>().from(INIT_STATE).to(ADD_INSTANCES_STATE).event(ADD_INSTANCES_EVENT).noFailureEvent().from(ADD_INSTANCES_STATE).to(ADD_INSTANCES_FINISHED_STATE).event(ADD_INSTANCES_FINISHED_EVENT).failureEvent(ADD_INSTANCES_FAILURE_EVENT).from(ADD_INSTANCES_FINISHED_STATE).to(EXTEND_METADATA_STATE).event(EXTEND_METADATA_EVENT).failureEvent(ADD_INSTANCES_FINISHED_FAILURE_EVENT).from(EXTEND_METADATA_STATE).to(EXTEND_METADATA_FINISHED_STATE).event(EXTEND_METADATA_FINISHED_EVENT).failureEvent(EXTEND_METADATA_FAILURE_EVENT).from(EXTEND_METADATA_FINISHED_STATE).to(BOOTSTRAP_NEW_NODES_STATE).event(StackUpscaleEvent.BOOTSTRAP_NEW_NODES_EVENT).failureEvent(StackUpscaleEvent.EXTEND_METADATA_FINISHED_FAILURE_EVENT).from(BOOTSTRAP_NEW_NODES_STATE).to(EXTEND_HOST_METADATA_STATE).event(StackUpscaleEvent.EXTEND_HOST_METADATA_EVENT).failureEvent(StackUpscaleEvent.BOOTSTRAP_NEW_NODES_FAILURE_EVENT).from(EXTEND_HOST_METADATA_STATE).to(EXTEND_HOST_METADATA_FINISHED_STATE).event(StackUpscaleEvent.EXTEND_HOST_METADATA_FINISHED_EVENT).failureEvent(StackUpscaleEvent.EXTEND_HOST_METADATA_FAILURE_EVENT).from(EXTEND_HOST_METADATA_FINISHED_STATE).to(FINAL_STATE).event(UPSCALE_FINALIZED_EVENT).failureEvent(StackUpscaleEvent.EXTEND_HOST_METADATA_FINISHED_FAILURE_EVENT).build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, UPSCALE_FAILED_STATE, UPSCALE_FAIL_HANDLED_EVENT)
    }
}
