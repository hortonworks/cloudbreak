package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.MUNCHAUSEN
import com.sequenceiq.cloudbreak.service.PollingResult.EXIT
import com.sequenceiq.cloudbreak.service.PollingResult.TIMEOUT

import java.util.ArrayList
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerBootstrapApiCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerClusterAvailabilityCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerBootstrapApiContext
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerOrchestratorClusterContext
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.PollingResult
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.TlsSecurityService

@Component
class ClusterBootstrapper {

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val orchestratorRepository: OrchestratorRepository? = null

    @Inject
    private val containerBootstrapApiPollingService: PollingService<ContainerBootstrapApiContext>? = null

    @Inject
    private val hostBootstrapApiPollingService: PollingService<HostBootstrapApiContext>? = null

    @Inject
    private val containerBootstrapApiCheckerTask: ContainerBootstrapApiCheckerTask? = null

    @Inject
    private val hostBootstrapApiCheckerTask: HostBootstrapApiCheckerTask? = null

    @Inject
    private val containerClusterAvailabilityPollingService: PollingService<ContainerOrchestratorClusterContext>? = null

    @Inject
    private val hostClusterAvailabilityPollingService: PollingService<HostOrchestratorClusterContext>? = null

    @Inject
    private val containerClusterAvailabilityCheckerTask: ContainerClusterAvailabilityCheckerTask? = null

    @Inject
    private val hostClusterAvailabilityCheckerTask: HostClusterAvailabilityCheckerTask? = null

    @Inject
    private val clusterBootstrapperErrorHandler: ClusterBootstrapperErrorHandler? = null

    @Inject
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null

    @Inject
    private val hostOrchestratorResolver: HostOrchestratorResolver? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Inject
    private val containerConfigService: ContainerConfigService? = null

    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null

    @Throws(CloudbreakException::class)
    fun bootstrapMachines(stackId: Long?) {
        val stack = stackRepository!!.findOneWithLists(stackId)
        val orchestratorType = orchestratorTypeResolver!!.resolveType(stack.orchestrator.type)

        if (orchestratorType.hostOrchestrator()) {
            bootstrapOnHost(stack)
        } else if (orchestratorType.containerOrchestrator()) {
            bootstrapContainers(stack)
        } else {
            LOGGER.error("Orchestrator not found: {}", stack.orchestrator.type)
            throw CloudbreakException("HostOrchestrator not found: " + stack.orchestrator.type)
        }
    }

    @SuppressWarnings("unchecked")
    @Throws(CloudbreakException::class)
    fun bootstrapOnHost(stack: Stack) {
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        val nodes = HashSet<Node>()
        for (instanceMetaData in stack.runningInstanceMetaData) {
            val node = Node(instanceMetaData.privateIp, instanceMetaData.publicIpWrapper)
            node.hostGroup = instanceMetaData.instanceGroupName
            nodes.add(node)
        }
        try {
            val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id, gatewayInstance.publicIpWrapper,
                    stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
            val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
            val bootstrapApiPolling = hostBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(
                    hostBootstrapApiCheckerTask,
                    HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS)
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.")
            hostOrchestrator.bootstrap(gatewayConfig, nodes, stack.consulServers, clusterDeletionBasedExitCriteriaModel(stack.id, null))

            val allNodesAvailabilityPolling = hostClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                    hostClusterAvailabilityCheckerTask,
                    HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS)
            validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.")
            val orchestrator = stack.orchestrator
            orchestrator.apiEndpoint = gatewayInstance.publicIpWrapper + ":" + stack.gatewayPort
            orchestrator.type = hostOrchestrator.name()
            orchestratorRepository!!.save(orchestrator)
            if (TIMEOUT == allNodesAvailabilityPolling) {
                clusterBootstrapperErrorHandler!!.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes)
            }
        } catch (e: Exception) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class)
    fun bootstrapContainers(stack: Stack) {
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()

        val nodes = HashSet<Node>()
        for (instanceMetaData in stack.runningInstanceMetaData) {
            nodes.add(Node(instanceMetaData.privateIp, instanceMetaData.publicIpWrapper))
        }
        try {
            val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id, gatewayInstance.publicIpWrapper,
                    stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
            val containerOrchestrator = containerOrchestratorResolver!!.get(SWARM)
            val bootstrapApiPolling = containerBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(
                    containerBootstrapApiCheckerTask,
                    ContainerBootstrapApiContext(stack, gatewayConfig, containerOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS)
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.")

            val nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator.maxBootstrapNodes, gatewayInstance.publicIpWrapper)
            containerOrchestrator.bootstrap(gatewayConfig, containerConfigService!!.get(stack, MUNCHAUSEN), nodeMap[0], stack.consulServers,
                    clusterDeletionBasedExitCriteriaModel(stack.id, null))
            if (nodeMap.size > 1) {
                val gatewayAvailability = containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                        containerClusterAvailabilityCheckerTask,
                        ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap[0]),
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS)
                validatePollingResultForCancellation(gatewayAvailability, "Polling of gateway node availability was cancelled.")
                for (i in 1..nodeMap.size - 1) {
                    containerOrchestrator.bootstrapNewNodes(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), nodeMap[i],
                            clusterDeletionBasedExitCriteriaModel(stack.id, null))
                    val agentAvailability = containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                            containerClusterAvailabilityCheckerTask,
                            ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap[i]),
                            POLLING_INTERVAL,
                            MAX_POLLING_ATTEMPTS)
                    validatePollingResultForCancellation(agentAvailability, "Polling of agent nodes availability was cancelled.")
                }
            }
            val allNodesAvailabilityPolling = containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                    containerClusterAvailabilityCheckerTask,
                    ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS)
            validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.")
            // TODO: put it in orchestrator api
            val orchestrator = Orchestrator()
            orchestrator.apiEndpoint = gatewayInstance.publicIpWrapper + ":" + stack.gatewayPort
            orchestrator.type = "SWARM"
            orchestratorRepository!!.save(orchestrator)
            stack.orchestrator = orchestrator
            stackRepository!!.save(stack)
            if (TIMEOUT == allNodesAvailabilityPolling) {
                clusterBootstrapperErrorHandler!!.terminateFailedNodes(null, containerOrchestrator, stack, gatewayConfig, nodes)
            }
        } catch (e: CloudbreakOrchestratorCancelledException) {
            throw CancellationException(e.message)
        } catch (e: CloudbreakOrchestratorException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class)
    fun bootstrapNewNodes(stackId: Long?, upscaleCandidateAddresses: Set<String>) {
        val stack = stackRepository!!.findOneWithLists(stackId)
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()

        val nodes = HashSet<Node>()
        for (instanceMetaData in stack.runningInstanceMetaData) {
            if (upscaleCandidateAddresses.contains(instanceMetaData.privateIp)) {
                nodes.add(Node(instanceMetaData.privateIp, instanceMetaData.publicIpWrapper))
            }
        }
        try {
            val gatewayConfig = tlsSecurityService!!.buildGatewayConfig(stack.id, gatewayInstance.publicIpWrapper,
                    stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
            val orchestratorType = orchestratorTypeResolver!!.resolveType(stack.orchestrator.type)
            if (orchestratorType.hostOrchestrator()) {
                bootstrapNewNodesOnHost(stack, gatewayInstance, nodes, gatewayConfig)
            } else if (orchestratorType.containerOrchestrator()) {
                bootstrapNewNodesInContainer(stack, gatewayInstance, nodes, gatewayConfig)
            }
        } catch (e: CloudbreakOrchestratorCancelledException) {
            throw CancellationException(e.message)
        } catch (e: CloudbreakOrchestratorException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class, CloudbreakOrchestratorException::class)
    private fun bootstrapNewNodesOnHost(stack: Stack, gatewayInstance: InstanceMetaData, nodes: Set<Node>, gatewayConfig: GatewayConfig) {
        val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
        val nodeMap = prepareBootstrapSegments(nodes, hostOrchestrator.maxBootstrapNodes, gatewayInstance.publicIpWrapper)
        val bootstrapApiPolling = hostBootstrapApiPollingService!!.pollWithTimeoutSingleFailure(
                hostBootstrapApiCheckerTask,
                HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS)
        validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.")
        val runningInstanceMetaData = stack.runningInstanceMetaData
        for (nodeSet in nodeMap) {
            nodeSet.forEach { n ->
                n.setHostGroup(
                        runningInstanceMetaData.stream().filter({ i -> i.getPrivateIp() == n.getPrivateIp() }).findFirst().get().getInstanceGroupName())
            }
            hostOrchestrator.bootstrapNewNodes(gatewayConfig, nodeSet, clusterDeletionBasedExitCriteriaModel(stack.id, null))
            val newNodesAvailabilityPolling = hostClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                    hostClusterAvailabilityCheckerTask,
                    HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodeSet),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS)
            validatePollingResultForCancellation(newNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.")
        }
        val allNodesAvailabilityPolling = hostClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                hostClusterAvailabilityCheckerTask,
                HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS)
        validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.")
        if (TIMEOUT == allNodesAvailabilityPolling) {
            clusterBootstrapperErrorHandler!!.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes)
        }
    }

    @Throws(CloudbreakException::class, CloudbreakOrchestratorException::class)
    private fun bootstrapNewNodesInContainer(stack: Stack, gatewayInstance: InstanceMetaData, nodes: Set<Node>, gatewayConfig: GatewayConfig) {
        val containerOrchestrator = containerOrchestratorResolver!!.get(SWARM)
        val nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator.maxBootstrapNodes, gatewayInstance.publicIpWrapper)
        for (i in nodeMap.indices) {
            containerOrchestrator.bootstrapNewNodes(gatewayConfig, containerConfigService!!.get(stack, MUNCHAUSEN), nodeMap[i],
                    clusterDeletionBasedExitCriteriaModel(stack.id, null))
            val newNodesAvailabilityPolling = containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                    containerClusterAvailabilityCheckerTask,
                    ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap[i]),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS)
            validatePollingResultForCancellation(newNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.")
        }
        val pollingResult = containerClusterAvailabilityPollingService!!.pollWithTimeoutSingleFailure(
                containerClusterAvailabilityCheckerTask,
                ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodes),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS)
        validatePollingResultForCancellation(pollingResult, "Polling of new nodes availability was cancelled.")
        if (TIMEOUT == pollingResult) {
            clusterBootstrapperErrorHandler!!.terminateFailedNodes(null, containerOrchestrator, stack, gatewayConfig, nodes)
        }
    }

    private fun prepareBootstrapSegments(nodes: Set<Node>, maxBootstrapNodes: Int, gatewayIp: String): List<Set<Node>> {
        val result = ArrayList<Set<Node>>()
        var newNodes: MutableSet<Node> = HashSet()
        val gatewayNode = getGateWayNode(nodes, gatewayIp)
        if (gatewayNode != null) {
            newNodes.add(gatewayNode)
        }
        for (node in nodes) {
            if (gatewayIp != node.publicIp) {
                newNodes.add(node)
                if (newNodes.size >= maxBootstrapNodes) {
                    result.add(newNodes)
                    newNodes = HashSet<Node>()
                }
            }
        }
        if (!newNodes.isEmpty()) {
            result.add(newNodes)
        }
        return result
    }

    private fun getGateWayNode(nodes: Set<Node>, gatewayIp: String): Node? {
        for (node in nodes) {
            if (gatewayIp == node.publicIp) {
                return node
            }
        }
        return null
    }

    private fun validatePollingResultForCancellation(pollingResult: PollingResult, cancelledMessage: String) {
        if (EXIT == pollingResult) {
            throw CancellationException(cancelledMessage)
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ClusterBootstrapper::class.java)

        private val POLLING_INTERVAL = 5000

        private val MAX_POLLING_ATTEMPTS = 500
    }

}
