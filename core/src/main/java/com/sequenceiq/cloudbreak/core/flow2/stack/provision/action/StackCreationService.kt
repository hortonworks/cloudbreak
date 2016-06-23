package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.CREATE_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS
import com.sequenceiq.cloudbreak.common.type.BillingStatus.BILLING_STOPPED
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE
import java.lang.String.format

import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.OnFailureAction
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.image.ImageService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.notification.Notification
import com.sequenceiq.cloudbreak.service.notification.NotificationSender
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class StackCreationService {

    @Inject
    private val stackService: StackService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val imageService: ImageService? = null
    @Inject
    private val notificationSender: NotificationSender? = null
    @Inject
    private val eventBus: EventBus? = null
    @Inject
    private val messagesService: CloudbreakMessagesService? = null
    @Inject
    private val cloudbreakEventService: CloudbreakEventService? = null
    @Inject
    private val connector: ServiceProviderConnectorAdapter? = null
    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null
    @Inject
    private val instanceMetadataService: InstanceMetadataService? = null
    @Inject
    private val metadatSetupService: MetadataSetupService? = null
    @Inject
    private val tlsSetupService: TlsSetupService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null

    fun startProvisioning(context: StackContext) {
        val stack = context.stack
        MDCBuilder.buildMdcContext(stack)
        stackUpdater!!.updateStackStatus(stack.id, CREATE_IN_PROGRESS, "Creating infrastructure")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_PROVISIONING, CREATE_IN_PROGRESS.name)
        instanceMetadataService!!.saveInstanceRequests(stack, context.cloudStack.groups)
    }

    fun provisioningFinished(context: StackContext, result: LaunchStackResult, variables: Map<Any, Any>): Stack {
        val startDate = getStartDateIfExist(variables)
        val stack = context.stack
        validateResourceResults(context.cloudContext, result)
        val results = result.results
        updateNodeCount(stack.id, context.cloudStack.groups, results, true)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_TIME, UPDATE_IN_PROGRESS.name, calculateStackCreationTime(startDate))
        return stackService!!.getById(stack.id)
    }

    private fun getStartDateIfExist(variables: Map<Any, Any>): Date {
        var result: Date? = null
        val startDateObj = variables[START_DATE]
        if (startDateObj != null && startDateObj is Date) {
            result = startDateObj as Date?
        }
        return result
    }

    fun checkImage(context: StackContext): CheckImageResult {
        try {
            val stack = context.stack
            val image = imageService!!.getImage(stack.id)
            val checkImageRequest = CheckImageRequest<CheckImageResult>(context.cloudContext, context.cloudCredential,
                    cloudStackConverter!!.convert(stack), image)
            LOGGER.info("Triggering event: {}", checkImageRequest)
            eventBus!!.notify(checkImageRequest.selector(), Event.wrap(checkImageRequest))

            val result = checkImageRequest.await()
            sendNotification(result, stack)
            LOGGER.info("Result: {}", result)
            return result
        } catch (e: InterruptedException) {
            LOGGER.error("Error while executing check image", e)
            throw OperationException(e)
        } catch (e: CloudbreakImageNotFoundException) {
            throw CloudbreakServiceException(e)
        }

    }

    fun setupMetadata(context: StackContext, collectMetadataResult: CollectMetadataResult): Stack {
        val stack = context.stack
        metadatSetupService!!.saveInstanceMetaData(stack, collectMetadataResult.results, InstanceStatus.CREATED)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.FLOW_STACK_PROVISIONED, BillingStatus.BILLING_STARTED.name)
        flowMessageService.fireEventAndLog(stack.id, Msg.FLOW_STACK_METADATA_COLLECTED, AVAILABLE.name)
        LOGGER.debug("Metadata setup DONE.")
        return stackService!!.getById(stack.id)
    }

    @Throws(CloudbreakException::class)
    fun setupTls(context: StackContext, sshFingerprints: GetSSHFingerprintsResult): Stack {
        LOGGER.info("Fingerprint has been determined: {}", sshFingerprints.sshFingerprints)
        val stack = context.stack
        val firstInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
        tlsSetupService!!.setupTls(stack, stack.gatewayInstanceGroup.instanceMetaData.iterator().next().publicIpWrapper,
                firstInstance.sshPort!!, stack.credential.loginUserName, sshFingerprints.sshFingerprints)
        return stackService!!.getById(stack.id)
    }

    fun bootstrappingMachines(stack: Stack) {
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_BOOTSTRAP, UPDATE_IN_PROGRESS.name)
    }

    fun collectingHostMetadata(stack: Stack) {
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_METADATA_SETUP, UPDATE_IN_PROGRESS.name)
    }

    fun stackCreationFinished(stack: Stack) {
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE)
    }

    fun handleStackCreationFailure(stack: Stack, errorDetails: Exception?) {
        MDCBuilder.buildMdcContext(stack)
        LOGGER.error("Error during stack creation flow:", errorDetails)
        val errorReason = if (errorDetails == null) "Unknown error" else errorDetails.message
        if (errorDetails is CancellationException || ExceptionUtils.getRootCause(errorDetails) is CancellationException) {
            LOGGER.warn("The flow has been cancelled.")
        } else {
            flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, UPDATE_IN_PROGRESS.name, errorReason)
            if (!stack.isStackInDeletionPhase) {
                handleFailure(stack, errorReason)
                stackUpdater!!.updateStackStatus(stack.id, CREATE_FAILED, errorReason)
                flowMessageService.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, CREATE_FAILED.name, errorReason)
            }
        }
    }

    private fun sendNotification(result: CheckImageResult, stack: Stack) {
        notificationSender!!.send(getImageCopyNotification(result, stack))
    }

    private fun getImageCopyNotification(result: CheckImageResult, stack: Stack): Notification {
        val notification = Notification()
        notification.eventType = "IMAGE_COPY_STATE"
        notification.eventTimestamp = Date()
        notification.eventMessage = result.statusProgressValue.toString()
        notification.owner = stack.owner
        notification.account = stack.account
        notification.cloud = stack.cloudPlatform().toString()
        notification.region = stack.region
        notification.stackId = stack.id
        notification.stackName = stack.name
        notification.stackStatus = stack.status
        return notification
    }

    private fun handleFailure(stack: Stack, errorReason: String) {
        try {
            if (stack.onFailureActionAction != OnFailureAction.ROLLBACK) {
                LOGGER.debug("Nothing to do. OnFailureAction {}", stack.onFailureActionAction)
            } else {
                // TODO Only trigger the rollback flow
                stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS)
                connector!!.rollback(stack, stack.resources)
                flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, BILLING_STOPPED.name, errorReason)
            }
        } catch (ex: Exception) {
            LOGGER.error("Stack rollback failed on stack id : {}. Exception:", stack.id, ex)
            stackUpdater!!.updateStackStatus(stack.id, CREATE_FAILED, String.format("Rollback failed: %s", ex.message))
            flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_ROLLBACK_FAILED, CREATE_FAILED.name, ex.message)
        }

    }

    private fun calculateStackCreationTime(startDate: Date?): Long {
        val result: Long = 0
        if (startDate != null) {
            return (Date().time - startDate.time) / DateUtils.MILLIS_PER_SECOND
        }
        return result
    }

    private fun validateResourceResults(cloudContext: CloudContext, res: LaunchStackResult) {
        validateResourceResults(cloudContext, res.errorDetails, res.results, true)
    }

    private fun validateResourceResults(cloudContext: CloudContext, exception: Exception?, results: List<CloudResourceStatus>, create: Boolean) {
        val action = if (create) "create" else "upscale"
        if (exception != null) {
            LOGGER.error(format("Failed to %s stack: %s", action, cloudContext), exception)
            throw OperationException(exception)
        }
        if (results.size == 1 && (results[0].isFailed || results[0].isDeleted)) {
            throw OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, results[0].statusReason))
        }
    }

    private fun updateNodeCount(stackId: Long?, originalGroups: List<Group>, statuses: List<CloudResourceStatus>, create: Boolean) {
        for (group in originalGroups) {
            val nodeCount = group.instances.size
            val failedResources = removeFailedMetadata(stackId, statuses, group)
            if (!failedResources.isEmpty() && create) {
                val failedCount = failedResources.size
                val instanceGroup = instanceGroupRepository!!.findOneByGroupNameInStack(stackId, group.name)
                instanceGroup.nodeCount = nodeCount - failedCount
                instanceGroupRepository.save(instanceGroup)
                flowMessageService!!.fireEventAndLog(stackId, Msg.STACK_INFRASTRUCTURE_ROLLBACK_MESSAGE, Status.UPDATE_IN_PROGRESS.name,
                        failedCount, group.name, failedResources[0].statusReason)
            }
        }
    }

    private fun removeFailedMetadata(stackId: Long?, statuses: List<CloudResourceStatus>, group: Group): List<CloudResourceStatus> {
        val failedResources = HashMap<Long, CloudResourceStatus>()
        val groupPrivateIds = getPrivateIds(group)
        for (status in statuses) {
            val privateId = status.privateId
            if (privateId != null && status.isFailed && !failedResources.containsKey(privateId) && groupPrivateIds.contains(privateId)) {
                failedResources.put(privateId, status)
                instanceMetadataService!!.deleteInstanceRequest(stackId, privateId)
            }
        }
        return ArrayList(failedResources.values)
    }

    private fun getPrivateIds(group: Group): Set<Long> {
        val ids = HashSet<Long>()
        for (cloudInstance in group.instances) {
            ids.add(cloudInstance.template!!.privateId)
        }
        return ids
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackCreationService::class.java)
    }
}
