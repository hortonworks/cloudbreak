package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.UUID

import javax.annotation.Resource
import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Payload

import reactor.bus.Event
import reactor.fn.Consumer

@Component
class FlowChainHandler : Consumer<Event<out Payload>> {
    @Resource
    private val flowChainConfigMap: Map<String, FlowEventChainFactory<Payload>>? = null

    @Inject
    private val flowChains: FlowChains? = null

    override fun accept(event: Event<out Payload>) {
        val key = event.key as String
        val flowEventChainFactory = flowChainConfigMap!![key]
        val flowChainId = UUID.randomUUID().toString()
        flowChains!!.putFlowChain(flowChainId, flowEventChainFactory.createFlowTriggerEventQueue(event.data))
        flowChains.triggerNextFlow(flowChainId)
    }
}
