package com.sequenceiq.cloudbreak.core.flow2.stack.upscale

import com.sequenceiq.cloudbreak.api.model.InstanceStatus.CREATED
import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS
import java.lang.String.format

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService

@Service
class StackUpscaleService {
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val stackScalingService: StackScalingService? = null
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val metadataConverter: InstanceMetaDataToCloudInstanceConverter? = null
    @Inject
    private val cloudResourceConverter: ResourceToCloudResourceConverter? = null
    @Inject
    private val instanceMetadataService: InstanceMetadataService? = null
    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null
    @Inject
    private val metadataSetupService: MetadataSetupService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val messagesService: CloudbreakMessagesService? = null
    @Inject
    private val eventService: CloudbreakEventService? = null
    @Inject
    private val hostGroupService: HostGroupService? = null
    @Inject
    private val cloudStackConverter: StackToCloudStackConverter? = null


    fun startAddInstances(stack: Stack, scalingAdjustment: Int?) {
        val statusReason = format("Adding %s new instance(s) to the infrastructure.", scalingAdjustment)
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS, statusReason)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_ADDING_INSTANCES, UPDATE_IN_PROGRESS.name, scalingAdjustment)
    }

    fun finishAddInstances(context: StackScalingFlowContext, payload: UpscaleStackResult): Set<Resource> {
        LOGGER.info("Upscale stack result: {}", payload)
        val results = payload.results
        validateResourceResults(context.cloudContext, payload.errorDetails, results)
        updateNodeCount(context.stack.id, context.cloudStack.groups, results, false)
        val resourceSet = transformResults(results, context.stack)
        if (resourceSet.isEmpty()) {
            throw OperationException("Failed to upscale the cluster since all create request failed: " + results.get(0).statusReason)
        }
        LOGGER.debug("Adding new instances to the stack is DONE")
        return resourceSet
    }

    fun finishExtendMetadata(stack: Stack, instanceGroupName: String, payload: CollectMetadataResult): Set<String> {
        val coreInstanceMetaData = payload.results
        metadataSetupService!!.saveInstanceMetaData(stack, coreInstanceMetaData, CREATED)
        val upscaleCandidateAddresses = HashSet<String>()
        for (cloudVmMetaDataStatus in coreInstanceMetaData) {
            upscaleCandidateAddresses.add(cloudVmMetaDataStatus.metaData.privateIp)
        }
        val instanceGroup = instanceGroupRepository!!.findOneByGroupNameInStack(stack.id, instanceGroupName)
        val nodeCount = instanceGroup.nodeCount!! + coreInstanceMetaData.size
        instanceGroup.nodeCount = nodeCount
        instanceGroupRepository.save(instanceGroup)
        clusterService!!.updateClusterStatusByStackId(stack.id, AVAILABLE)
        eventService!!.fireCloudbreakEvent(stack.id, BillingStatus.BILLING_CHANGED.name,
                messagesService!!.getMessage("stack.metadata.setup.billing.changed"))
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_METADATA_EXTEND, AVAILABLE.name)

        return upscaleCandidateAddresses
    }

    fun finishExtendHostMetadata(stack: Stack) {
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Stack upscale has been finished successfully.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_UPSCALE_FINISHED, AVAILABLE.name)
    }

    fun handleStackUpscaleFailure(stack: Stack, payload: StackFailureEvent) {
        LOGGER.error("Exception during the downscaling of stack", payload.exception)
        try {
            val errorReason = payload.exception.message
            stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Stack update failed. " + errorReason)
            flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, AVAILABLE.name, errorReason)
        } catch (e: Exception) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.message)
        }

    }

    private fun transformResults(cloudResourceStatuses: List<CloudResourceStatus>, stack: Stack): Set<Resource> {
        val retSet = HashSet<Resource>()
        for (cloudResourceStatus in cloudResourceStatuses) {
            if (!cloudResourceStatus.isFailed) {
                val cloudResource = cloudResourceStatus.cloudResource
                val resource = Resource(cloudResource.type, cloudResource.name, cloudResource.reference, cloudResource.status,
                        stack, null)
                retSet.add(resource)
            }
        }
        return retSet
    }

    private fun validateResourceResults(cloudContext: CloudContext, exception: Exception?, results: List<CloudResourceStatus>) {
        if (exception != null) {
            LOGGER.error(format("Failed to upscale stack: %s", cloudContext), exception)
            throw OperationException(exception)
        }
        if (results.size == 1 && (results[0].isFailed || results[0].isDeleted)) {
            throw OperationException(format("Failed to upscale the stack for %s due to: %s", cloudContext, results[0].statusReason))
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

    fun getNewInstances(stack: Stack): List<CloudInstance> {
        val cloudInstances = cloudStackConverter!!.buildInstances(stack)
        val iterator = cloudInstances.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().template!!.status !== InstanceStatus.CREATE_REQUESTED) {
                iterator.remove()
            }
        }
        return cloudInstances
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackUpscaleService::class.java)
    }
}
