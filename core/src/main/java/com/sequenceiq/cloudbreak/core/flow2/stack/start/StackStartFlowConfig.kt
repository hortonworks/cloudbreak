package com.sequenceiq.cloudbreak.core.flow2.stack.start

import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.COLLECT_METADATA_FAILED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.COLLECT_METADATA_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAILURE_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.COLLECTING_METADATA
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class StackStartFlowConfig : AbstractFlowConfiguration<StackStartState, StackStartEvent>(StackStartState::class.java, StackStartEvent::class.java) {

    override val events: Array<StackStartEvent>
        get() = StackStartEvent.values()

    override val initEvents: Array<StackStartEvent>
        get() = arrayOf(StackStartEvent.START_EVENT)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<StackStartState, StackStartEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<StackStartState, StackStartEvent>
        get() = EDGE_CONFIG

    companion object {

        private val TRANSITIONS = Transition.Builder<StackStartState, StackStartEvent>().defaultFailureEvent(START_FAILURE_EVENT).from(INIT_STATE).to(START_STATE).event(START_EVENT).noFailureEvent().from(START_STATE).to(COLLECTING_METADATA).event(START_FINISHED_EVENT).defaultFailureEvent().from(COLLECTING_METADATA).to(START_FINISHED_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT).from(START_FINISHED_STATE).to(FINAL_STATE).event(START_FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, START_FAILED_STATE, START_FAIL_HANDLED_EVENT)
    }
}
