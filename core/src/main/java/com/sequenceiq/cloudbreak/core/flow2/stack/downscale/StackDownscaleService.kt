package com.sequenceiq.cloudbreak.core.flow2.stack.downscale

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService

@Service
class StackDownscaleService {
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null
    @Inject
    private val stackScalingService: StackScalingService? = null


    fun startStackDownscale(context: StackScalingFlowContext, adjustment: Int?) {
        LOGGER.debug("Downscaling of stack ", context.stack.id)
        MDCBuilder.buildMdcContext(context.stack)
        stackUpdater!!.updateStackStatus(context.stack.id, UPDATE_IN_PROGRESS)
        flowMessageService!!.fireEventAndLog(context.stack.id, Msg.STACK_DOWNSCALE_INSTANCES, UPDATE_IN_PROGRESS.name, Math.abs(adjustment!!))
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun finishStackDownscale(context: StackScalingFlowContext, instanceGroupName: String, instanceIds: Set<String>) {
        val stack = context.stack
        stackScalingService!!.updateRemovedResourcesState(stack, instanceIds, stack.getInstanceGroupByInstanceGroupName(instanceGroupName))
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Downscale of the cluster infrastructure finished successfully.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_DOWNSCALE_SUCCESS, AVAILABLE.name)

        if (stack.cluster != null && stack.cluster.emailNeeded!!) {
            emailSenderService!!.sendDownScaleSuccessEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
            flowMessageService.fireEventAndLog(context.stack.id, Msg.STACK_NOTIFICATION_EMAIL, AVAILABLE.name)
        }
    }

    fun handleStackDownscaleError(errorDetails: Exception) {
        LOGGER.error("Exception during the downscaling of stack", errorDetails)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackDownscaleService::class.java)
    }
}
