package com.sequenceiq.cloudbreak.core.flow2.stack.sync

import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FAIL_HANDLED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FINALIZED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FINISHED_EVENT
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.FINAL_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.INIT_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_FAILED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_FINISHED_STATE
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_STATE

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration

@Component
class StackSyncFlowConfig : AbstractFlowConfiguration<StackSyncState, StackSyncEvent>(StackSyncState::class.java, StackSyncEvent::class.java) {

    override val events: Array<StackSyncEvent>
        get() = StackSyncEvent.values()

    override val initEvents: Array<StackSyncEvent>
        get() = arrayOf(StackSyncEvent.SYNC_EVENT)

    protected override val transitions: List<AbstractFlowConfiguration.Transition<StackSyncState, StackSyncEvent>>
        get() = TRANSITIONS

    protected override val edgeConfig: AbstractFlowConfiguration.FlowEdgeConfig<StackSyncState, StackSyncEvent>
        get() = EDGE_CONFIG

    companion object {
        private val TRANSITIONS = Transition.Builder<StackSyncState, StackSyncEvent>().defaultFailureEvent(StackSyncEvent.SYNC_FAILURE_EVENT).from(INIT_STATE).to(SYNC_STATE).event(SYNC_EVENT).noFailureEvent().from(SYNC_STATE).to(SYNC_FINISHED_STATE).event(SYNC_FINISHED_EVENT).defaultFailureEvent().from(SYNC_FINISHED_STATE).to(FINAL_STATE).event(SYNC_FINALIZED_EVENT).defaultFailureEvent().build()

        private val EDGE_CONFIG = AbstractFlowConfiguration.FlowEdgeConfig(INIT_STATE, FINAL_STATE, SYNC_FAILED_STATE, SYNC_FAIL_HANDLED_EVENT)
    }
}
