package com.sequenceiq.cloudbreak.core.flow2.cluster

import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowState
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.StackService

abstract class AbstractClusterAction<P : Payload> protected constructor(payloadClass: Class<P>) : AbstractAction<FlowState, FlowEvent, ClusterContext, P>(payloadClass) {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val clusterService: ClusterService? = null

    override fun createFlowContext(flowId: String, clusterContext: StateContext<FlowState, FlowEvent>, payload: P): ClusterContext {
        val stack = stackService!!.getById(payload.stackId)
        val cluster = clusterService!!.retrieveClusterByStackId(payload.stackId)
        // TODO LogAspect!!
        MDCBuilder.buildMdcContext(cluster)
        return ClusterContext(flowId, stack, cluster)
    }

    override fun getFailurePayload(payload: P, flowContext: Optional<ClusterContext>, ex: Exception): Any {
        return StackFailureEvent(payload.stackId, ex)
    }
}
