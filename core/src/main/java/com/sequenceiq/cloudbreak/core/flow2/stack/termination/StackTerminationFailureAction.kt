package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

@Component("StackTerminationFailureAction")
class StackTerminationFailureAction : AbstractStackFailureAction<StackTerminationState, StackTerminationEvent>() {
    @Inject
    private val stackTerminationService: StackTerminationService? = null

    override fun doExecute(context: StackFailureContext, payload: StackFailureEvent, variables: Map<Any, Any>) {
        stackTerminationService!!.handleStackTerminationError(context.stack, payload, variables["FORCEDTERMINATION"] != null)
        sendEvent(context)
    }

    override fun createRequest(context: StackFailureContext): Selectable {
        return StackEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation(), context.stack.id)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction::class.java)
    }
}
