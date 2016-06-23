package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

@Component
class SyncFlowEventChainFactory : FlowEventChainFactory<StackEvent> {
    override fun initEvent(): String {
        return FlowTriggers.FULL_SYNC_TRIGGER_EVENT
    }

    override fun createFlowTriggerEventQueue(event: StackEvent): Queue<Selectable> {
        val flowEventChain = ConcurrentLinkedQueue<Selectable>()
        flowEventChain.add(StackSyncTriggerEvent(FlowTriggers.STACK_SYNC_TRIGGER_EVENT, event.stackId, true))
        flowEventChain.add(StackEvent(FlowTriggers.CLUSTER_SYNC_TRIGGER_EVENT, event.stackId))
        return flowEventChain
    }
}
