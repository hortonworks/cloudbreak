package com.sequenceiq.cloudbreak.core.flow2

import java.util.UUID

import javax.annotation.Resource
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService

import reactor.bus.Event
import reactor.bus.EventBus
import reactor.fn.Consumer

@Component
class Flow2Handler : Consumer<Event<out Payload>> {

    @Inject
    private val flowLogService: FlowLogService? = null

    @Resource
    private val flowConfigurationMap: Map<String, FlowConfiguration<*>>? = null

    @Inject
    private val flowChains: FlowChains? = null

    @Inject
    private val runningFlows: FlowRegister? = null

    @Inject
    private val eventBus: EventBus? = null

    override fun accept(event: Event<out Payload>) {
        val key = event.key as String
        val payload = event.data
        var flowId: String? = getFlowId(event)
        val flowChainId = getFlowChainId(event)

        if (FLOW_CANCEL == key) {
            cancelRunningFlows(payload.stackId)
        } else if (FLOW_FINAL == key) {
            finalizeFlow(flowId, flowChainId, payload.stackId, event)
        } else {
            if (flowId == null) {
                LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload)
                // TODO this is needed because we have two flow implementations in the same time and we want to avoid conflicts
                val flowConfig = flowConfigurationMap!![key]
                if (flowConfig != null && flowConfig.flowTriggerCondition.isFlowTriggerable(payload.stackId)) {
                    flowId = UUID.randomUUID().toString()
                    val flow = flowConfig.createFlow(flowId)
                    runningFlows!!.put(flow, flowChainId)
                    flow.initialize()
                    flowLogService!!.save(flowId, key, payload, flowConfig.javaClass, flow.currentState)
                    flow.sendEvent(key, payload)
                }
            } else {
                LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload)
                val flow = runningFlows!!.get(flowId)
                if (flow != null) {
                    flowLogService!!.save(flowId, key, payload, flow.flowConfigClass, flow.currentState)
                    flow.sendEvent(key, payload)
                } else {
                    LOGGER.info("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.stackId, flowId, key)
                }
            }
        }
    }

    private fun cancelRunningFlows(stackId: Long?) {
        val flowIds = flowLogService!!.findAllRunningNonTerminationFlowIdsByStackId(stackId)
        LOGGER.debug("flow cancellation arrived: ids: {}", flowIds)
        for (id in flowIds) {
            val flow = runningFlows!!.remove(id)
            if (flow != null) {
                flowLogService.cancel(stackId, id)
                flowChains!!.removeFlowChain(runningFlows.getFlowChainId(id))
            }
        }
    }

    private fun finalizeFlow(flowId: String, flowChainId: String?, stackId: Long?, event: Event<out Payload>) {
        LOGGER.debug("flow finalizing arrived: id: {}", flowId)
        flowLogService!!.close(stackId, flowId)
        val flow = runningFlows!!.remove(flowId)
        if (flow.isFlowFailed) {
            flowChains!!.removeFlowChain(flowChainId)
        } else if (flowChainId != null) {
            flowChains!!.triggerNextFlow(flowChainId)
        }
    }

    private fun getFlowId(event: Event<*>): String {
        return event.headers.get<String>("FLOW_ID")
    }

    private fun getFlowChainId(event: Event<*>): String {
        return event.headers.get<String>("FLOW_CHAIN_ID")
    }

    companion object {
        val FLOW_FINAL = "FLOWFINAL"
        val FLOW_CANCEL = "FLOWCANCEL"

        private val LOGGER = LoggerFactory.getLogger(Flow2Handler::class.java)
    }
}
