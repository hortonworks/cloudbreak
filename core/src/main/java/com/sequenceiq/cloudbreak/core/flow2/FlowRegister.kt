package com.sequenceiq.cloudbreak.core.flow2

import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import org.springframework.stereotype.Component

@Component
class FlowRegister {
    private val runningFlows = ConcurrentHashMap<String, Pair<Flow, String>>()

    fun put(flow: Flow, chainFlowId: String) {
        runningFlows.put(flow.flowId, ImmutablePair(flow, chainFlowId))
    }

    operator fun get(flowId: String): Flow {
        return runningFlows[flowId].getLeft()
    }

    fun getFlowChainId(flowId: String): String {
        return runningFlows[flowId].getRight()
    }

    fun remove(flowId: String): Flow? {
        val pair = runningFlows.remove(flowId)
        return pair?.left
    }
}
