package com.sequenceiq.cloudbreak.core.flow2.stack

import java.util.Optional

import javax.inject.Inject

import org.springframework.statemachine.StateContext

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction
import com.sequenceiq.cloudbreak.core.flow2.Flow
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowState
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.service.stack.StackService

abstract class AbstractStackFailureAction<S : FlowState, E : FlowEvent> protected constructor() : AbstractAction<S, E, StackFailureContext, StackFailureEvent>(StackFailureEvent::class.java) {
    @Inject
    private val stackService: StackService? = null

    override fun createFlowContext(flowId: String, stateContext: StateContext<S, E>, payload: StackFailureEvent): StackFailureContext {
        val flow = getFlow(flowId)
        val stack = stackService!!.getById(payload.stackId)
        MDCBuilder.buildMdcContext(stack)
        flow.setFlowFailed()
        return StackFailureContext(flowId, stack)
    }

    override fun getFailurePayload(payload: StackFailureEvent, flowContext: Optional<StackFailureContext>, ex: Exception): Any? {
        return null
    }

    override fun initPayloadConverterMap(payloadConverters: MutableList<PayloadConverter<StackFailureEvent>>) {
        payloadConverters.add(CloudPlatformResponseToStackFailureConverter())
        payloadConverters.add(ClusterPlatformResponseToStackFailureConverter())
    }
}
