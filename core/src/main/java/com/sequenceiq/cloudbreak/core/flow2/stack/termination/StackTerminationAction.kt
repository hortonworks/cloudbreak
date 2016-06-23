package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService

@Component("StackTerminationAction")
open class StackTerminationAction : AbstractStackTerminationAction<StackEvent>(StackEvent::class.java) {
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val messagesService: CloudbreakMessagesService? = null
    @Inject
    private val cloudbreakEventService: CloudbreakEventService? = null
    @Inject
    private val terminationService: TerminationService? = null

    override fun doExecute(context: StackTerminationContext, payload: StackEvent, variables: Map<Any, Any>) {
        doExecute(context)
    }

    override fun createRequest(context: StackTerminationContext): TerminateStackRequest<Any> {
        return TerminateStackRequest(context.cloudContext, context.cloudStack, context.cloudCredential, context.cloudResources)
    }

    protected fun doExecute(context: StackTerminationContext) {
        val terminateRequest = createRequest(context)
        val stack = context.stack
        if (stack == null || stack.credential == null) {
            LOGGER.info("Could not trigger stack event on null", terminateRequest)
            val statusReason = "Stack or credential not found."
            val terminateStackResult = TerminateStackResult(statusReason, IllegalArgumentException(statusReason), terminateRequest)
            sendEvent(context.flowId, StackTerminationEvent.TERMINATION_FAILED_EVENT.stringRepresentation(), terminateStackResult)
        } else {
            stackUpdater!!.updateStackStatus(stack.id, DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.")
            cloudbreakEventService!!.fireCloudbreakEvent(context.stack.id, DELETE_IN_PROGRESS.name,
                    messagesService!!.getMessage(Msg.STACK_DELETE_IN_PROGRESS.code()))
            LOGGER.debug("Assembling terminate stack event for stack: {}", stack)
            LOGGER.info("Triggering terminate stack event: {}", terminateRequest)
            sendEvent(context.flowId, terminateRequest.selector(), terminateRequest)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackTerminationAction::class.java)
    }
}
