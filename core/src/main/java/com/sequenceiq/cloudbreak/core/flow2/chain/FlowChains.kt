package com.sequenceiq.cloudbreak.core.flow2.chain

import java.util.HashMap
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class FlowChains {

    @Inject
    private val eventBus: EventBus? = null
    private val flowChainMap = ConcurrentHashMap<String, Queue<Selectable>>()

    fun putFlowChain(flowChainId: String, flowChain: Queue<Selectable>) {
        flowChainMap.put(flowChainId, flowChain)
    }

    fun removeFlowChain(flowChainId: String?) {
        if (flowChainId != null) {
            flowChainMap.remove(flowChainId)
        }
    }

    fun triggerNextFlow(flowChainId: String) {
        val queue = flowChainMap[flowChainId]
        if (queue != null) {
            val selectable = queue.poll()
            if (selectable != null) {
                sendEvent(flowChainId, selectable)
            } else {
                removeFlowChain(flowChainId)
            }
        }
    }

    protected fun sendEvent(flowChainId: String, selectable: Selectable) {
        LOGGER.info("Triggering event: {}", selectable)
        val headers = HashMap<String, Any>()
        headers.put("FLOW_CHAIN_ID", flowChainId)
        eventBus!!.notify(selectable.selector(), Event(Event.Headers(headers), selectable))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FlowChains::class.java)
    }
}
