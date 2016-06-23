package com.sequenceiq.cloudbreak.core.flow2

import java.util.Optional

import org.springframework.statemachine.StateContext
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.event.Selectable

@Component("FlowFinalizeAction")
class FlowFinalizeAction : AbstractAction<FlowState, FlowEvent, CommonContext, Payload>(Payload::class.java) {

    override fun createFlowContext(flowId: String, stateContext: StateContext<FlowState, FlowEvent>, payload: Payload): CommonContext {
        return CommonContext(flowId)
    }

    override fun doExecute(context: CommonContext, payload: Payload, variables: Map<Any, Any>) {
        sendEvent(context.flowId, Flow2Handler.FLOW_FINAL, payload)
    }

    override fun createRequest(context: CommonContext): Selectable? {
        return null
    }

    override fun getFailurePayload(payload: Payload, flowContext: Optional<CommonContext>, ex: Exception): Any? {
        return null
    }
}
