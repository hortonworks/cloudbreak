package com.sequenceiq.cloudbreak.core.flow2.stack.start

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.START_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.STOPPED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService
import com.sequenceiq.cloudbreak.service.stack.flow.WrongMetadataException

@Service
class StackStartStopService {

    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val metadatSetupService: MetadataSetupService? = null

    fun startStackStart(context: StackStartStopContext) {
        val stack = context.stack
        MDCBuilder.buildMdcContext(stack)
        stackUpdater!!.updateStackStatus(stack.id, Status.START_IN_PROGRESS, "Cluster infrastructure is now starting.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_STARTING, Status.START_IN_PROGRESS.name)
    }

    fun finishStackStart(stack: Stack, coreInstanceMetaData: List<CloudVmMetaDataStatus>) {
        if (coreInstanceMetaData.size != stack.fullNodeCount) {
            throw WrongMetadataException(String.format(
                    "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                    coreInstanceMetaData.size, stack.fullNodeCount))
        }
        metadatSetupService!!.saveInstanceMetaData(stack, coreInstanceMetaData, null)
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Cluster infrastructure started successfully.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_STARTED, AVAILABLE.name)
        flowMessageService.fireEventAndLog(stack.id, Msg.STACK_BILLING_STARTED, BillingStatus.BILLING_STARTED.name)
    }

    fun handleStackStartError(stack: Stack, payload: StackFailureEvent) {
        handleError(stack, payload.exception, START_FAILED, Msg.STACK_INFRASTRUCTURE_START_FAILED, "Stack start failed: ")
    }

    fun startStackStop(context: StackStartStopContext) {
        val stack = context.stack
        if (isStopPossible(stack)) {
            MDCBuilder.buildMdcContext(stack)
            stackUpdater!!.updateStackStatus(context.stack.id, Status.STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.")
            flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_STOPPING, Status.STOP_IN_PROGRESS.name)
        }
    }

    fun finishStackStop(context: StackStartStopContext) {
        val stack = context.stack
        stackUpdater!!.updateStackStatus(stack.id, Status.STOPPED, "Cluster infrastructure stopped successfully.")

        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_STOPPED, Status.STOPPED.name)
        flowMessageService.fireEventAndLog(stack.id, Msg.STACK_BILLING_STOPPED, BillingStatus.BILLING_STOPPED.name)

        if (stack.cluster != null && stack.cluster.emailNeeded!!) {
            emailSenderService!!.sendStopSuccessEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.STACK_NOTIFICATION_EMAIL, Status.STOPPED.name)
        }
    }

    fun handleStackStopError(stack: Stack, payload: StackFailureEvent) {
        handleError(stack, payload.exception, STOP_FAILED, Msg.STACK_INFRASTRUCTURE_STOP_FAILED, "Stack stop failed: ")
    }

    fun isStopPossible(stack: Stack?): Boolean {
        if (stack != null && stack.isStopRequested) {
            return true
        } else {
            LOGGER.info("Stack stop has not been requested, stop stack later.")
            return false
        }
    }

    private fun handleError(stack: Stack, exception: Exception, stackStatus: Status, msg: Msg, logMessage: String) {
        LOGGER.error(logMessage, exception)
        stackUpdater!!.updateStackStatus(stack.id, stackStatus, logMessage + exception.message)
        flowMessageService!!.fireEventAndLog(stack.id, msg, stackStatus.name, exception.message)
        if (stack.cluster != null) {
            clusterService!!.updateClusterStatusByStackId(stack.id, STOPPED)
            if (stack.cluster.emailNeeded!!) {
                emailSenderService!!.sendStopFailureEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
                flowMessageService.fireEventAndLog(stack.id, Msg.STACK_NOTIFICATION_EMAIL, stackStatus.name)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackStartStopService::class.java)
    }
}
