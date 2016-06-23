package com.sequenceiq.cloudbreak.service.stack.flow

import java.util.Arrays
import java.util.Calendar
import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter

@Service
class StackScalingService {

    @Inject
    private val stackService: StackService? = null
    @Inject
    private val eventService: CloudbreakEventService? = null
    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null
    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null
    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null
    @Inject
    private val ambariDecommissioner: AmbariDecommissioner? = null
    @Inject
    private val connector: ServiceProviderConnectorAdapter? = null
    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    private enum class Msg private constructor(private val code: String) {
        STACK_SCALING_HOST_DELETED("stack.scaling.host.deleted"),
        STACK_SCALING_HOST_DELETE_FAILED("stack.scaling.host.delete.failed"),
        STACK_SCALING_HOST_NOT_FOUND("stack.scaling.host.not.found"),
        STACK_SCALING_BILLING_CHANGED("stack.scaling.billing.changed");

        fun code(): String {
            return code
        }
    }

    fun removeHostmetadataIfExists(stack: Stack, instanceMetaData: InstanceMetaData, hostMetadata: HostMetadata?) {
        if (hostMetadata != null) {
            try {
                ambariDecommissioner!!.deleteHostFromAmbari(stack, hostMetadata)
                hostMetadataRepository!!.delete(hostMetadata)
                eventService!!.fireCloudbreakEvent(stack.id, Status.AVAILABLE.name,
                        cloudbreakMessagesService!!.getMessage(Msg.STACK_SCALING_HOST_DELETED.code(),
                                Arrays.asList(instanceMetaData.instanceId)))
            } catch (e: Exception) {
                LOGGER.error("Host cannot be deleted from cluster: ", e)
                eventService!!.fireCloudbreakEvent(stack.id, Status.DELETE_FAILED.name,
                        cloudbreakMessagesService!!.getMessage(Msg.STACK_SCALING_HOST_DELETE_FAILED.code(),
                                Arrays.asList(instanceMetaData.instanceId)))

            }

        } else {
            LOGGER.info("Host cannot be deleted because it is not exist: ", instanceMetaData.instanceId)
            eventService!!.fireCloudbreakEvent(stack.id, Status.AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.STACK_SCALING_HOST_NOT_FOUND.code(),
                            Arrays.asList(instanceMetaData.instanceId)))

        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun updateRemovedResourcesState(stack: Stack, instanceIds: Set<String>, instanceGroup: InstanceGroup) {
        val nodeCount = instanceGroup.nodeCount!! - instanceIds.size
        instanceGroup.nodeCount = nodeCount
        instanceGroupRepository!!.save(instanceGroup)

        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)

        for (instanceMetaData in instanceGroup.instanceMetaData) {
            if (instanceIds.contains(instanceMetaData.instanceId)) {
                val timeInMillis = Calendar.getInstance().timeInMillis
                instanceMetaData.terminationDate = timeInMillis
                instanceMetaData.instanceStatus = InstanceStatus.TERMINATED
                instanceMetaDataRepository!!.save(instanceMetaData)
            }
        }
        LOGGER.info("Successfully terminated metadata of instances '{}' in stack.", instanceIds)
        eventService!!.fireCloudbreakEvent(stack.id, BillingStatus.BILLING_CHANGED.name,
                cloudbreakMessagesService!!.getMessage(Msg.STACK_SCALING_BILLING_CHANGED.code()))
    }

    fun getUnusedInstanceIds(instanceGroupName: String, scalingAdjustment: Int?, stack: Stack): Map<String, String> {
        val instanceIds = HashMap<String, String>()
        var i = 0
        for (metaData in stack.getInstanceGroupByInstanceGroupName(instanceGroupName).instanceMetaData) {
            if (!metaData.ambariServer && (metaData.isDecommissioned || metaData.isUnRegistered || metaData.isCreated || metaData.isFailed)) {
                instanceIds.put(metaData.instanceId, metaData.discoveryFQDN)
                if (++i >= scalingAdjustment!! * -1) {
                    break
                }
            }
        }
        return instanceIds
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackScalingService::class.java)
        private val POLLING_INTERVAL = 5000
        private val MAX_POLLING_ATTEMPTS = 100
    }

}
