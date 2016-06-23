package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class InstanceTerminationFlowConfig : AbstractFlowConfiguration<InstanceTerminationState, InstanceTerminationEvent>(InstanceTerminationState::class.java, InstanceTerminationEvent::class.java) {

    override val flowTriggerCondition: FlowTriggerCondition
        get() = applicationContext.getBean<InstanceTerminationFlowTriggerCondition>(InstanceTerminationFlowTriggerCondition::class.java)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<InstanceTerminationState, InstanceTerminationEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<InstanceTerminationState, InstanceTerminationEvent>
        get() = EDGE_CONFIG

    override val events: Array<InstanceTerminationEvent>
        get() = InstanceTerminationEvent.values()

    override val initEvents: Array<InstanceTerminationEvent>
        get() = arrayOf(InstanceTerminationEvent.TERMINATION_EVENT)

    companion object {

        private val TRANSITIONS = Transition.Builder<InstanceTerminationState, InstanceTerminationEvent>().defaultFailureEvent(TERMINATION_FAILED_EVENT).from(INIT_STATE).to(TERMINATION_STATE).event(TERMINATION_EVENT).noFailureEvent().from(TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent().from(TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, TERMINATION_FAILED_STATE, TERMINATION_FAIL_HANDLED_EVENT)
    }
}
