package com.sequenceiq.cloudbreak.service.stack.flow

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.STOPPED
import com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC

import java.util.Arrays
import java.util.Calendar
import java.util.HashMap
import java.util.Optional

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.common.type.HostMetadataState
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.ResourceRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter

@Service
class StackSyncService {

    @Inject
    private val stackService: StackService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val eventService: CloudbreakEventService? = null
    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null
    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null
    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null
    @Inject
    private val resourceRepository: ResourceRepository? = null
    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null
    @Inject
    private val ambariDecommissioner: AmbariDecommissioner? = null
    @Inject
    private val metadata: ServiceProviderMetadataAdapter? = null
    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    private enum class Msg private constructor(private val code: String) {
        STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED("stack.sync.instance.status.retrieval.failed"),
        STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE("stack.sync.instance.status.couldnt.determine"),
        STACK_SYNC_INSTANCE_OPERATION_IN_PROGRESS("stack.sync.instance.operation.in.progress"),
        STACK_SYNC_INSTANCE_STOPPED_ON_PROVIDER("stack.sync.instance.stopped.on.provider"),
        STACK_SYNC_INSTANCE_STATE_SYNCED("stack.sync.instance.state.synced"),
        STACK_SYNC_HOST_DELETED("stack.sync.host.deleted"),
        STACK_SYNC_INSTANCE_REMOVAL_FAILED("stack.sync.instance.removal.failed"),
        STACK_SYNC_HOST_UPDATED("stack.sync.host.updated"),
        STACK_SYNC_INSTANCE_TERMINATED("stack.sync.instance.terminated"),
        STACK_SYNC_INSTANCE_DELETED_CBMETADATA("stack.sync.instance.deleted.cbmetadata"),
        STACK_SYNC_INSTANCE_UPDATED("stack.sync.instance.updated"),
        STACK_SYNC_INSTANCE_FAILED("stack.sync.instance.failed");

        fun code(): String {
            return code
        }
    }

    fun updateInstances(stack: Stack, instanceMetaDataList: List<InstanceMetaData>, instanceStatuses: List<CloudVmInstanceStatus>,
                        stackStatusUpdateEnabled: Boolean) {
        val counts = initInstanceStateCounts()
        for (metaData in instanceMetaDataList) {
            val status = instanceStatuses.stream().filter({ `is` ->
                `is` != null && `is`!!.cloudInstance.instanceId != null
                        && `is`!!.cloudInstance.instanceId == metaData.instanceId
            }).findFirst()

            val state = if (!status.isPresent()) InstanceSyncState.DELETED else transform(status.get().status)
            syncInstanceStatusByState(stack, counts, metaData, state)
        }

        handleSyncResult(stack, counts, stackStatusUpdateEnabled)
    }

    fun sync(stackId: Long?, stackStatusUpdateEnabled: Boolean) {
        val stack = stackService!!.getById(stackId)
        if (stack.isStackInDeletionPhase || stack.isModificationInProgress) {
            LOGGER.warn("Stack could not be synchronized in {} state!", stack.status)
        } else {
            sync(stack, stackStatusUpdateEnabled)
        }
    }

    private fun sync(stack: Stack, stackStatusUpdateEnabled: Boolean) {
        val stackId = stack.id
        val instances = instanceMetaDataRepository!!.findNotTerminatedForStack(stackId)
        val instanceStateCounts = initInstanceStateCounts()
        for (instance in instances) {
            val instanceGroup = instance.instanceGroup
            try {
                val state = metadata!!.getState(stack, instanceGroup, instance.instanceId)
                syncInstanceStatusByState(stack, instanceStateCounts, instance, state)
            } catch (e: CloudConnectorException) {
                LOGGER.warn(e.message, e)
                eventService!!.fireCloudbreakEvent(stackId, AVAILABLE.name,
                        cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_STATUS_RETRIEVAL_FAILED.code(), Arrays.asList(instance.instanceId)))
                instanceStateCounts.put(InstanceSyncState.UNKNOWN, instanceStateCounts[InstanceSyncState.UNKNOWN] + 1)
            }

        }
        handleSyncResult(stack, instanceStateCounts, stackStatusUpdateEnabled)
    }

    private fun syncInstanceStatusByState(stack: Stack, counts: MutableMap<InstanceSyncState, Int>, metaData: InstanceMetaData, state: InstanceSyncState) {
        if (InstanceSyncState.DELETED == state && !metaData.isTerminated) {
            syncDeletedInstance(stack, counts, metaData)
        } else if (InstanceSyncState.RUNNING == state) {
            syncRunningInstance(stack, counts, metaData)
        } else if (InstanceSyncState.STOPPED == state) {
            syncStoppedInstance(stack, counts, metaData)
        } else {
            counts.put(InstanceSyncState.IN_PROGRESS, counts[InstanceSyncState.IN_PROGRESS] + 1)
        }
    }

    private fun syncStoppedInstance(stack: Stack, instanceStateCounts: MutableMap<InstanceSyncState, Int>, instance: InstanceMetaData) {
        instanceStateCounts.put(InstanceSyncState.STOPPED, instanceStateCounts[InstanceSyncState.STOPPED] + 1)
        if (!instance.isTerminated && !stack.isStopped) {
            LOGGER.info("Instance '{}' is reported as stopped on the cloud provider, setting its state to STOPPED.", instance.instanceId)
            instance.instanceStatus = InstanceStatus.STOPPED
            instanceMetaDataRepository!!.save(instance)
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_UPDATED.code(), Arrays.asList(instance.instanceId, "stopped")))
        }
    }

    private fun syncRunningInstance(stack: Stack, instanceStateCounts: MutableMap<InstanceSyncState, Int>, instance: InstanceMetaData) {
        instanceStateCounts.put(InstanceSyncState.RUNNING, instanceStateCounts[InstanceSyncState.RUNNING] + 1)
        if (stack.status == WAIT_FOR_SYNC && instance.isCreated) {
            LOGGER.info("Instance '{}' is reported as created on the cloud provider but not member of the cluster, setting its state to FAILED.",
                    instance.instanceId)
            instance.instanceStatus = InstanceStatus.FAILED
            instanceMetaDataRepository!!.save(instance)
            eventService!!.fireCloudbreakEvent(stack.id, CREATE_FAILED.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_FAILED.code(), Arrays.asList(instance.discoveryFQDN)))
        } else if (!instance.isRunning && !instance.isDecommissioned && !instance.isCreated && !instance.isFailed) {
            LOGGER.info("Instance '{}' is reported as running on the cloud provider, updating metadata.", instance.instanceId)
            updateMetaDataToRunning(stack.id, stack.cluster, instance)
        }
    }

    private fun syncDeletedInstance(stack: Stack, instanceStateCounts: MutableMap<InstanceSyncState, Int>, instance: InstanceMetaData) {
        instanceStateCounts.put(InstanceSyncState.DELETED, instanceStateCounts[InstanceSyncState.DELETED] + 1)
        deleteHostFromCluster(stack, instance)
        if (!instance.isTerminated) {
            LOGGER.info("Instance '{}' is reported as deleted on the cloud provider, setting its state to TERMINATED.", instance.instanceId)
            deleteResourceIfNeeded(stack, instance)
            updateMetaDataToTerminated(stack, instance)
        }
    }

    private fun deleteResourceIfNeeded(stack: Stack, instance: InstanceMetaData) {
        val resource = resourceRepository!!.findByStackIdAndResourceNameOrReference(stack.id, instance.instanceId)
        if (resource != null) {
            resourceRepository.delete(resource)
        }
    }

    private fun handleSyncResult(stack: Stack, instanceStateCounts: Map<InstanceSyncState, Int>, stackStatusUpdateEnabled: Boolean) {
        val instances = instanceMetaDataRepository!!.findNotTerminatedForStack(stack.id)
        if (instanceStateCounts[InstanceSyncState.UNKNOWN] > 0) {
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE.code()))
        } else if (instanceStateCounts[InstanceSyncState.IN_PROGRESS] > 0) {
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_OPERATION_IN_PROGRESS.code()))
        } else if (instanceStateCounts[InstanceSyncState.RUNNING] > 0 && instanceStateCounts[InstanceSyncState.STOPPED] > 0) {
            eventService!!.fireCloudbreakEvent(stack.id, STOPPED.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_STOPPED_ON_PROVIDER.code()))
        } else if (instanceStateCounts[InstanceSyncState.RUNNING] > 0) {
            updateStackStatusIfEnabled(stack.id, AVAILABLE, SYNC_STATUS_REASON, stackStatusUpdateEnabled)
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()))
        } else if (instanceStateCounts[InstanceSyncState.STOPPED] == instances.size) {
            updateStackStatusIfEnabled(stack.id, STOPPED, SYNC_STATUS_REASON, stackStatusUpdateEnabled)
            eventService!!.fireCloudbreakEvent(stack.id, STOPPED.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()))
        } else {
            updateStackStatusIfEnabled(stack.id, DELETE_FAILED, SYNC_STATUS_REASON, stackStatusUpdateEnabled)
            eventService!!.fireCloudbreakEvent(stack.id, DELETE_FAILED.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_STATE_SYNCED.code()))
        }
    }

    private fun updateStackStatusIfEnabled(stackId: Long?, status: Status, statusReason: String, stackStatusUpdateEnabled: Boolean) {
        if (stackStatusUpdateEnabled) {
            stackUpdater!!.updateStackStatus(stackId, status, statusReason)
        }
    }

    private fun transform(instanceStatus: com.sequenceiq.cloudbreak.cloud.model.InstanceStatus): InstanceSyncState {
        when (instanceStatus) {
            InstanceStatus.IN_PROGRESS -> return InstanceSyncState.IN_PROGRESS
            InstanceStatus.STARTED -> return InstanceSyncState.RUNNING
            InstanceStatus.STOPPED -> return InstanceSyncState.STOPPED
            InstanceStatus.CREATED -> return InstanceSyncState.RUNNING
            InstanceStatus.FAILED -> return InstanceSyncState.DELETED
            InstanceStatus.TERMINATED -> return InstanceSyncState.DELETED
            else -> return InstanceSyncState.UNKNOWN
        }
    }

    private fun initInstanceStateCounts(): MutableMap<InstanceSyncState, Int> {
        val instanceStates = HashMap<InstanceSyncState, Int>()
        instanceStates.put(InstanceSyncState.DELETED, 0)
        instanceStates.put(InstanceSyncState.STOPPED, 0)
        instanceStates.put(InstanceSyncState.RUNNING, 0)
        instanceStates.put(InstanceSyncState.IN_PROGRESS, 0)
        instanceStates.put(InstanceSyncState.UNKNOWN, 0)
        return instanceStates
    }

    private fun deleteHostFromCluster(stack: Stack, instanceMetaData: InstanceMetaData) {
        try {
            if (stack.cluster != null) {
                val hostMetadata = hostMetadataRepository!!.findHostInClusterByName(stack.cluster.id, instanceMetaData.discoveryFQDN) ?: throw NotFoundException(String.format("Host not found with id '%s'", instanceMetaData.discoveryFQDN))
                if (ambariClusterConnector!!.isAmbariAvailable(stack)) {
                    if (ambariDecommissioner!!.deleteHostFromAmbari(stack, hostMetadata)) {
                        hostMetadataRepository.delete(hostMetadata.id)
                        eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                                cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_HOST_DELETED.code(), Arrays.asList(instanceMetaData.discoveryFQDN)))
                    } else {
                        eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                                cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_REMOVAL_FAILED.code(),
                                        Arrays.asList(instanceMetaData.discoveryFQDN)))
                    }
                } else {
                    hostMetadata.hostMetadataState = HostMetadataState.UNHEALTHY
                    hostMetadataRepository.save(hostMetadata)
                    eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                            cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_HOST_UPDATED.code(),
                                    Arrays.asList(instanceMetaData.discoveryFQDN, HostMetadataState.UNHEALTHY.name)))
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Host cannot be deleted from cluster: ", e)
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_TERMINATED.code(), Arrays.asList(instanceMetaData.discoveryFQDN)))
        }

    }

    private fun updateMetaDataToTerminated(stack: Stack, instanceMetaData: InstanceMetaData) {
        val instanceGroup = instanceMetaData.instanceGroup
        instanceGroup.nodeCount = instanceGroup.nodeCount!! - 1
        val timeInMillis = Calendar.getInstance().timeInMillis
        instanceMetaData.terminationDate = timeInMillis
        instanceMetaData.instanceStatus = InstanceStatus.TERMINATED
        instanceMetaDataRepository!!.save(instanceMetaData)
        instanceGroupRepository!!.save(instanceGroup)
        eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_DELETED_CBMETADATA.code(), Arrays.asList(instanceMetaData.discoveryFQDN)))
    }

    private fun updateMetaDataToRunning(stackId: Long?, cluster: Cluster, instanceMetaData: InstanceMetaData) {
        val instanceGroup = instanceMetaData.instanceGroup
        if (InstanceStatus.TERMINATED == instanceMetaData.instanceStatus) {
            instanceGroup.nodeCount = instanceGroup.nodeCount!! + 1
        }
        val hostMetadata = hostMetadataRepository!!.findHostInClusterByName(cluster.id, instanceMetaData.discoveryFQDN)
        if (hostMetadata != null) {
            LOGGER.info("Instance '{}' was found in the cluster metadata, setting it's state to REGISTERED.", instanceMetaData.instanceId)
            instanceMetaData.instanceStatus = InstanceStatus.REGISTERED
        } else {
            LOGGER.info("Instance '{}' was not found in the cluster metadata, setting it's state to UNREGISTERED.", instanceMetaData.instanceId)
            instanceMetaData.instanceStatus = InstanceStatus.UNREGISTERED
        }
        instanceMetaDataRepository!!.save(instanceMetaData)
        instanceGroupRepository!!.save(instanceGroup)
        eventService!!.fireCloudbreakEvent(stackId, AVAILABLE.name,
                cloudbreakMessagesService!!.getMessage(Msg.STACK_SYNC_INSTANCE_UPDATED.code(), Arrays.asList(instanceMetaData.discoveryFQDN, "running")))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackSyncService::class.java)

        private val SYNC_STATUS_REASON = "Synced instance states with the cloud provider."
    }


}