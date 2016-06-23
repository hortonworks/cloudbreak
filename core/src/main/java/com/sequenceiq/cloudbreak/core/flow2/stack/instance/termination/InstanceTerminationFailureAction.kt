package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

@Component("InstanceTerminationFailureAction")
class InstanceTerminationFailureAction : AbstractStackFailureAction<InstanceTerminationState, InstanceTerminationEvent>() {
    @Inject
    private val instanceTerminationService: InstanceTerminationService? = null

    override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
        instanceTerminationService!!.handleInstanceTerminationError(context.stack, payload)
        sendEvent(context)
    }

    override fun createRequest(context: StackFailureContext): Selectable {
        return StackEvent(InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
    }
}
