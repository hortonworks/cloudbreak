package com.sequenceiq.cloudbreak.core.flow2.cluster.reset

import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.StackService

abstract class AbstractClusterResetAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<ClusterResetState, ClusterResetEvent, ClusterContext, P>(payloadClass) {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val clusterService: ClusterService? = null

    override fun createFlowContext(flowId: String, stateContext: StateContext<ClusterResetState, ClusterResetEvent>, payload: P): ClusterContext {
        val stack = stackService!!.getById(payload.stackId)
        val cluster = clusterService!!.retrieveClusterByStackId(stack.id)
        MDCBuilder.buildMdcContext(stack)
        return ClusterContext(flowId, stack, cluster)
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<ClusterContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }
}
