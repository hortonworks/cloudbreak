package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS

import java.util.Collections

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult
import com.sequenceiq.cloudbreak.common.type.HostMetadataState
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.stack.flow.ScalingFailedException
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService

@Service
class InstanceTerminationService {
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val stackScalingService: StackScalingService? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null

    fun instanceTermination(context: InstanceTerminationContext) {
        val stack = context.stack
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS, "Removing instance")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_REMOVING_INSTANCE, UPDATE_IN_PROGRESS.name)
        val instanceMetaData = context.instanceMetaData
        val hostName = instanceMetaData.discoveryFQDN
        if (stack.cluster != null) {
            val hostMetadata = hostMetadataRepository!!.findHostInClusterByName(stack.cluster.id, hostName)
            if (hostMetadata != null && HostMetadataState.HEALTHY == hostMetadata.hostMetadataState) {
                throw ScalingFailedException(String.format("Host (%s) is in HEALTHY state. Cannot be removed.", hostName))
            }
        }
        val instanceGroupName = instanceMetaData.instanceGroup.groupName
        flowMessageService.fireEventAndLog(stack.id, Msg.STACK_SCALING_TERMINATING_HOST_FROM_HOSTGROUP, UPDATE_IN_PROGRESS.name,
                hostName, instanceGroupName)
    }

    @Throws(Exception::class)
    fun finishInstanceTermination(context: InstanceTerminationContext, payload: RemoveInstanceResult) {
        val stack = context.stack
        val instanceMetaData = context.instanceMetaData
        val instanceId = instanceMetaData.instanceId
        val instanceGroup = stack.getInstanceGroupByInstanceGroupId(instanceMetaData.instanceGroup.id)
        stackScalingService!!.updateRemovedResourcesState(stack, setOf<String>(instanceId), instanceGroup)
        if (stack.cluster != null) {
            val hostMetadata = hostMetadataRepository!!.findHostInClusterByName(stack.cluster.id, instanceMetaData.discoveryFQDN)
            if (hostMetadata != null) {
                LOGGER.info("Remove obsolete host: {}", hostMetadata.hostName)
                stackScalingService.removeHostmetadataIfExists(stack, instanceMetaData, hostMetadata)
            }
        }
        LOGGER.info("Terminate instance result: {}", payload)
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Instance removed")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_REMOVING_INSTANCE_FINISHED, AVAILABLE.name)
    }

    fun handleInstanceTerminationError(stack: Stack, payload: StackFailureEvent) {
        val ex = payload.exception
        LOGGER.error("Error during instance terminating flow:", ex)
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Instance termination failed. " + ex.message)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.STACK_REMOVING_INSTANCE_FAILED, AVAILABLE.name, ex.message)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InstanceTerminationService::class.java)
    }
}
