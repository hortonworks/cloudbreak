package com.sequenceiq.cloudbreak.core.bootstrap.service

import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.ResourceRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter

@Component
class ClusterBootstrapperErrorHandler {

    @Inject
    private val resourceRepository: ResourceRepository? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null

    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null

    @Inject
    private val eventService: CloudbreakEventService? = null

    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Inject
    private val connector: ServiceProviderConnectorAdapter? = null

    @Inject
    private val metadata: ServiceProviderMetadataAdapter? = null

    private enum class Msg private constructor(private val code: String) {

        BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES("bootstrapper.error.nodes.failed"),
        BOOTSTRAPPER_ERROR_DELETING_NODE("bootstrapper.error.deleting.node"),
        BOOTSTRAPPER_ERROR_INVALID_NODECOUNT("bootstrapper.error.invalide.nodecount");

        fun code(): String {
            return code
        }
    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun terminateFailedNodes(hostOrchestrator: HostOrchestrator?, containerOrchestrator: ContainerOrchestrator,
                             stack: Stack, gatewayConfig: GatewayConfig, nodes: Set<Node>) {
        val allAvailableNode: List<String>
        if (hostOrchestrator != null) {
            allAvailableNode = hostOrchestrator.getAvailableNodes(gatewayConfig, nodes)
        } else {
            allAvailableNode = containerOrchestrator.getAvailableNodes(gatewayConfig, nodes)
        }
        val missingNodes = selectMissingNodes(nodes, allAvailableNode)
        if (missingNodes.size > 0) {
            var message = cloudbreakMessagesService!!.getMessage(Msg.BOOTSTRAPPER_ERROR_BOOTSTRAP_FAILED_ON_NODES.code(), Arrays.asList(missingNodes.size))
            LOGGER.info(message)
            eventService!!.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name, message)

            for (missingNode in missingNodes) {
                val instanceMetaData = instanceMetaDataRepository!!.findNotTerminatedByPrivateAddress(stack.id, missingNode.privateIp)
                val ig = instanceGroupRepository!!.findOneByGroupNameInStack(stack.id, instanceMetaData.instanceGroup.groupName)
                ig.nodeCount = ig.nodeCount!! - 1
                if (ig.nodeCount < 1) {
                    throw CloudbreakOrchestratorFailedException(cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_INVALID_NODECOUNT.code(),
                            Arrays.asList(ig.groupName)))
                }
                instanceGroupRepository.save(ig)
                message = cloudbreakMessagesService.getMessage(Msg.BOOTSTRAPPER_ERROR_DELETING_NODE.code(),
                        Arrays.asList(instanceMetaData.instanceId, ig.groupName))
                LOGGER.info(message)
                eventService.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name, message)
                deleteResourceAndDependencies(stack, instanceMetaData)
                deleteInstanceResourceFromDatabase(stack, instanceMetaData)
                val timeInMillis = Calendar.getInstance().timeInMillis
                instanceMetaData.terminationDate = timeInMillis
                instanceMetaData.instanceStatus = InstanceStatus.TERMINATED
                instanceMetaDataRepository.save(instanceMetaData)
                LOGGER.info("InstanceMetadata [name: {}, id: {}] status set to {}.", instanceMetaData.id, instanceMetaData.instanceId,
                        instanceMetaData.instanceStatus)
            }
        }
    }

    private fun selectMissingNodes(clusterNodes: Set<Node>, availableNodes: List<String>): List<Node> {
        val missingNodes = ArrayList<Node>()
        for (node in clusterNodes) {
            var contains = false
            for (nodeAddress in availableNodes) {
                if (nodeAddress == node.privateIp) {
                    contains = true
                    break
                }
            }
            if (!contains) {
                missingNodes.add(node)
            }
        }
        return missingNodes
    }

    private fun deleteResourceAndDependencies(stack: Stack, instanceMetaData: InstanceMetaData) {
        LOGGER.info("Rolling back instance [name: {}, id: {}]", instanceMetaData.id, instanceMetaData.instanceId)
        val instanceIds = HashSet<String>()
        instanceIds.add(instanceMetaData.instanceId)
        connector!!.removeInstances(stack, instanceIds, instanceMetaData.instanceGroup.groupName)
        LOGGER.info("Deleted instance [name: {}, id: {}]", instanceMetaData.id, instanceMetaData.instanceId)
    }

    private fun deleteInstanceResourceFromDatabase(stack: Stack, instanceMetaData: InstanceMetaData) {
        val resource = resourceRepository!!.findByStackIdAndNameAndType(stack.id, instanceMetaData.instanceId, null)
        if (resource != null) {
            resourceRepository.delete(resource.id)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterBootstrapperErrorHandler::class.java)
    }
}
