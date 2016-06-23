package com.sequenceiq.cloudbreak.core.flow2.cluster.sync

import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.stack.StackService

abstract class AbstractClusterSyncAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<ClusterSyncState, ClusterSyncEvent, ClusterSyncContext, P>(payloadClass) {
    @Inject
    private val stackService: StackService? = null

    override fun createFlowContext(flowId: String, stateContext: StateContext<ClusterSyncState, ClusterSyncEvent>, payload: P): ClusterSyncContext {
        val stack = stackService!!.getById(payload.stackId)
        MDCBuilder.buildMdcContext(stack)
        return ClusterSyncContext(flowId, stack)
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<ClusterSyncContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }
}
