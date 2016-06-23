package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.INIT_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class StackDownscaleConfig : AbstractFlowConfiguration<StackDownscaleState, StackDownscaleEvent>(StackDownscaleState::class.java, StackDownscaleEvent::class.java) {

    override val events: Array<StackDownscaleEvent>
        get() = StackDownscaleEvent.values()

    override val initEvents: Array<StackDownscaleEvent>
        get() = arrayOf(StackDownscaleEvent.DOWNSCALE_EVENT)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<StackDownscaleState, StackDownscaleEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<StackDownscaleState, StackDownscaleEvent>
        get() = EDGE_CONFIG

    companion object {
        private val TRANSITIONS = Transition.Builder<StackDownscaleState, StackDownscaleEvent>().defaultFailureEvent(DOWNSCALE_FAILURE_EVENT).from(INIT_STATE).to(DOWNSCALE_STATE).event(DOWNSCALE_EVENT).noFailureEvent().from(DOWNSCALE_STATE).to(DOWNSCALE_FINISHED_STATE).event(DOWNSCALE_FINISHED_EVENT).defaultFailureEvent().from(DOWNSCALE_FINISHED_STATE).to(FINAL_STATE).event(DOWNSCALE_FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, DOWNSCALE_FAILED_STATE, DOWNSCALE_FAIL_HANDLED_EVENT)
    }
}
