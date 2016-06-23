package com.sequenceiq.cloudbreak.service.flowlog

import javax.inject.Inject
import javax.transaction.Transactional

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.core.flow2.FlowState
import com.sequenceiq.cloudbreak.domain.FlowLog
import com.sequenceiq.cloudbreak.repository.FlowLogRepository

@Service
@Transactional
class FlowLogService {

    @Inject
    private val flowLogRepository: FlowLogRepository? = null

    fun save(flowId: String, key: String, payload: Payload, flowType: Class<*>, currentState: FlowState): FlowLog {
        val flowLog = FlowLog(payload.stackId, flowId, key, null, payload.javaClass, flowType, currentState.toString())
        return flowLogRepository!!.save(flowLog)
    }

    fun close(stackId: Long?, flowId: String): FlowLog {
        return finalize(stackId, flowId, "FINISHED")
    }

    fun cancel(stackId: Long?, flowId: String): FlowLog {
        return finalize(stackId, flowId, "CANCELLED")
    }

    fun terminate(stackId: Long?, flowId: String): FlowLog {
        return finalize(stackId, flowId, "TERMINATED")
    }

    private fun finalize(stackId: Long?, flowId: String, state: String): FlowLog {
        flowLogRepository!!.finalizeByFlowId(flowId)
        val flowLog = FlowLog(stackId, flowId, state, java.lang.Boolean.TRUE)
        return flowLogRepository.save(flowLog)
    }

    fun findAllRunningNonTerminationFlowIdsByStackId(stackId: Long?): Set<String> {
        return flowLogRepository!!.findAllRunningNonTerminationFlowIdsByStackId(stackId)
    }
}
