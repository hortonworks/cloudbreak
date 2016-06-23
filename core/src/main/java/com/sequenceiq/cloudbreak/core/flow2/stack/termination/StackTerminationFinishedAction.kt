package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

@Component("StackTerminationFinishedAction")
class StackTerminationFinishedAction : AbstractStackTerminationAction<TerminateStackResult>(TerminateStackResult::class.java) {
    @Inject
    private val stackTerminationService: StackTerminationService? = null

    override fun doExecute(context: StackTerminationContext, payload: TerminateStackResult, variables: Map<Any, Any>) {
        stackTerminationService!!.finishStackTermination(context, payload)
        sendEvent(context)
    }

    override fun createRequest(context: StackTerminationContext): Selectable {
        return StackEvent(StackTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation(), context.stack.id)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackTerminationFinishedAction::class.java)
    }
}
