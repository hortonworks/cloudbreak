package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

@Component
class StartFlowEventChainFactory : FlowEventChainFactory<StackEvent> {

    override fun initEvent(): String {
        return FlowTriggers.FULL_START_TRIGGER_EVENT
    }

    override fun createFlowTriggerEventQueue(event: StackEvent): Queue<Selectable> {
        val flowEventChain = ConcurrentLinkedQueue<Selectable>()
        flowEventChain.add(StackEvent(FlowTriggers.STACK_START_TRIGGER_EVENT, event.stackId))
        flowEventChain.add(StackEvent(FlowTriggers.CLUSTER_START_TRIGGER_EVENT, event.stackId))
        return flowEventChain
    }
}
