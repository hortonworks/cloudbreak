package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult
import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService

@Service
class StackTerminationService {
    @Inject
    private val terminationService: TerminationService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null

    fun finishStackTermination(context: StackTerminationContext, payload: TerminateStackResult) {
        LOGGER.info("Terminate stack result: {}", payload)
        val stack = context.stack
        terminationService!!.finalizeTermination(stack.id, true)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_BILLING_STOPPED, BillingStatus.BILLING_STOPPED.name)
        flowMessageService.fireEventAndLog(stack.id, Msg.STACK_DELETE_COMPLETED, DELETE_COMPLETED.name)
        clusterService!!.updateClusterStatusByStackId(stack.id, DELETE_COMPLETED)
        if (stack.cluster != null && stack.cluster.emailNeeded!!) {
            emailSenderService!!.sendTerminationSuccessEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.STACK_NOTIFICATION_EMAIL, DELETE_COMPLETED.name)
        }
    }

    fun handleStackTerminationError(stack: Stack, payload: StackFailureEvent, forced: Boolean) {
        val stackUpdateMessage: String
        val eventMessage: Msg
        val status: Status
        if (!forced) {
            val errorDetails = payload.exception
            stackUpdateMessage = "Termination failed: " + errorDetails.message
            status = Status.DELETE_FAILED
            eventMessage = Msg.STACK_INFRASTRUCTURE_DELETE_FAILED
            stackUpdater!!.updateStackStatus(stack.id, status, stackUpdateMessage)
            LOGGER.error("Error during stack termination flow: ", errorDetails)
        } else {
            terminationService!!.finalizeTermination(stack.id, true)
            clusterService!!.updateClusterStatusByStackId(stack.id, DELETE_COMPLETED)
            stackUpdateMessage = "Stack was force terminated."
            status = DELETE_COMPLETED
            eventMessage = Msg.STACK_FORCED_DELETE_COMPLETED
        }
        flowMessageService!!.fireEventAndLog(stack.id, eventMessage, status.name, stackUpdateMessage)
        if (stack.cluster != null && stack.cluster.emailNeeded!!) {
            if (forced) {
                emailSenderService!!.sendTerminationSuccessEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
            } else {
                emailSenderService!!.sendTerminationFailureEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
            }
            flowMessageService.fireEventAndLog(stack.id, Msg.STACK_NOTIFICATION_EMAIL, status.name)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackTerminationService::class.java)
    }
}
