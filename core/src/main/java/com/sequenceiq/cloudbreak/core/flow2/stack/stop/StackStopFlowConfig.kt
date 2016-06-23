package com.sequenceiq.cloudbreak.core.flow2.stack.stop

import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class StackStopFlowConfig : AbstractFlowConfiguration<StackStopState, StackStopEvent>(StackStopState::class.java, StackStopEvent::class.java) {

    override val flowTriggerCondition: FlowTriggerCondition
        get() = applicationContext.getBean<StackStopFlowTriggerCondition>(StackStopFlowTriggerCondition::class.java)

    override val events: Array<StackStopEvent>
        get() = StackStopEvent.values()

    override val initEvents: Array<StackStopEvent>
        get() = arrayOf(StackStopEvent.STOP_EVENT)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<StackStopState, StackStopEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<StackStopState, StackStopEvent>
        get() = EDGE_CONFIG

    companion object {

        private val TRANSITIONS = Transition.Builder<StackStopState, StackStopEvent>().defaultFailureEvent(StackStopEvent.STOP_FAILURE_EVENT).from(INIT_STATE).to(STOP_STATE).event(STOP_EVENT).noFailureEvent().from(STOP_STATE).to(STOP_FINISHED_STATE).event(STOP_FINISHED_EVENT).defaultFailureEvent().from(STOP_FINISHED_STATE).to(FINAL_STATE).event(STOP_FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, STOP_FAILED_STATE, STOP_FAIL_HANDLED_EVENT)
    }
}
