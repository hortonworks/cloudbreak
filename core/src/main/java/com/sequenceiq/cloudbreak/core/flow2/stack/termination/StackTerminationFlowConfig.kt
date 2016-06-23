package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.FORCE_TERMINATION_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.FORCE_TERMINATION_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState.TERMINATION_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class StackTerminationFlowConfig : AbstractFlowConfiguration<StackTerminationState, StackTerminationEvent>(StackTerminationState::class.java, StackTerminationEvent::class.java) {

    protected override val transitions: List<AbstractFlowConfiguration.Transition<StackTerminationState, StackTerminationEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<StackTerminationState, StackTerminationEvent>
        get() = EDGE_CONFIG

    override val events: Array<StackTerminationEvent>
        get() = StackTerminationEvent.values()

    override val initEvents: Array<StackTerminationEvent>
        get() = arrayOf(StackTerminationEvent.TERMINATION_EVENT, StackTerminationEvent.FORCE_TERMINATION_EVENT)

    companion object {

        private val TRANSITIONS = Transition.Builder<StackTerminationState, StackTerminationEvent>().defaultFailureEvent(TERMINATION_FAILED_EVENT).from(INIT_STATE).to(TERMINATION_STATE).event(TERMINATION_EVENT).noFailureEvent().from(INIT_STATE).to(FORCE_TERMINATION_STATE).event(FORCE_TERMINATION_EVENT).noFailureEvent().from(TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent().from(FORCE_TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent().from(TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, TERMINATION_FAILED_STATE, STACK_TERMINATION_FAIL_HANDLED_EVENT)
    }
}
