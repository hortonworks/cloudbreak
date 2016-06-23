package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT
import com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS
import com.sequenceiq.cloudbreak.service.PollingResult.isExited
import com.sequenceiq.cloudbreak.service.PollingResult.isSuccess
import com.sequenceiq.cloudbreak.service.PollingResult.isTimeout
import com.sequenceiq.cloudbreak.service.cluster.DataNodeUtils.sortByUsedSpace
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.DECOMMISSION_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.STOP_SERVICES_AMBARI_PROGRESS_STATE
import java.util.Arrays.asList
import java.util.Collections.singletonMap

import java.net.ConnectException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.ContainerRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.PollingResult
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider
import com.sequenceiq.cloudbreak.service.cluster.AmbariConfigurationService
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil

import groovyx.net.http.HttpResponseException

@Component
class AmbariDecommissioner {

    @Inject
    private val stackRepository: StackRepository? = null
    @Inject
    private val clusterRepository: ClusterRepository? = null
    @Inject
    private val hostGroupService: HostGroupService? = null
    @Inject
    private val ambariClientProvider: AmbariClientProvider? = null
    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null
    @Inject
    private val eventService: CloudbreakEventService? = null
    @Inject
    private val rsPollerService: PollingService<AmbariHostsWithNames>? = null
    @Inject
    private val ambariClientPollingService: PollingService<AmbariClientPollerObject>? = null
    @Inject
    private val dnDecommissionStatusCheckerTask: DNDecommissionStatusCheckerTask? = null
    @Inject
    private val rsDecommissionStatusCheckerTask: RSDecommissionStatusCheckerTask? = null
    @Inject
    private val hostsLeaveStatusCheckerTask: AmbariHostsLeaveStatusCheckerTask? = null
    @Inject
    private val ambariHostLeave: PollingService<AmbariHostsWithNames>? = null
    @Inject
    private val ambariOperationService: AmbariOperationService? = null
    @Inject
    private val configurationService: AmbariConfigurationService? = null
    @Inject
    private val hostFilterService: HostFilterService? = null
    @Inject
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null
    @Inject
    private val containerRepository: ContainerRepository? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null
    @Inject
    private val hostOrchestratorResolver: HostOrchestratorResolver? = null

    private enum class Msg private constructor(private val code: String) {
        AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP("ambari.cluster.removing.node.from.hostgroup");

        fun code(): String {
            return code
        }
    }

    @Throws(CloudbreakException::class)
    fun decommissionAmbariNodes(stack: Stack, hostGroupName: String, scalingAdjustment: Int?): Set<String> {
        var cluster = stack.cluster
        val adjustment = Math.abs(scalingAdjustment!!)
        val hostsToRemove = selectHostsToRemove(collectDownscaleCandidates(stack, cluster, hostGroupName, adjustment), adjustment)
        if (hostsToRemove.size != adjustment) {
            throw CloudbreakException(String.format("Only %d hosts found to downscale but %d required.", hostsToRemove.size, adjustment))
        }
        val result = HashSet<String>()
        LOGGER.info("Decommissioning {} hosts from host group '{}'", adjustment, hostGroupName)
        eventService!!.fireCloudbreakInstanceGroupEvent(stack.id, Status.UPDATE_IN_PROGRESS.name,
                cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP.code(), asList(adjustment, hostGroupName)), hostGroupName)
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        val ambariClient = ambariClientProvider!!.getSecureAmbariClient(clientConfig, stack.gatewayPort, cluster)
        val blueprintName = stack.cluster.blueprint.blueprintName
        var pollingResult = startServicesIfNeeded(stack, ambariClient, blueprintName)
        if (isSuccess(pollingResult)) {
            val components = getHadoopComponents(ambariClient, hostGroupName, blueprintName)
            val hostList = ArrayList(hostsToRemove.keys)
            pollingResult = ambariOperationService!!.waitForOperations(stack, ambariClient, decommissionComponents(ambariClient, hostList, components),
                    DECOMMISSION_AMBARI_PROGRESS_STATE)
            if (isSuccess(pollingResult)) {
                pollingResult = waitForDataNodeDecommission(stack, ambariClient)
                if (isSuccess(pollingResult)) {
                    //TODO https://hortonworks.jira.com/browse/BUG-53283
                    //TODO for now we only wait for the Ambari request to finish
                    //pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, components);
                    if (isSuccess(pollingResult)) {
                        pollingResult = stopHadoopComponents(stack, ambariClient, hostList)
                        if (isSuccess(pollingResult)) {
                            stopAndDeleteHosts(stack, ambariClient, hostList, components)
                            cluster = clusterRepository!!.findOneWithLists(stack.cluster.id)
                            val hostGroup = hostGroupService!!.getByClusterIdAndName(cluster.id, hostGroupName)
                            hostGroup.hostMetadata.removeAll(hostsToRemove.values)
                            hostGroupService.save(hostGroup)
                            result.addAll(hostsToRemove.keys)
                        }
                    }
                }
            }
        }
        return result
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun deleteHostFromAmbari(stack: Stack, data: HostMetadata): Boolean {
        var hostDeleted = false
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, stack.cluster.ambariIp)
        val ambariClient = ambariClientProvider!!.getSecureAmbariClient(clientConfig, stack.gatewayPort, stack.cluster)
        val components = getHadoopComponents(ambariClient, data.hostGroup.name, stack.cluster.blueprint.blueprintName)
        if (ambariClient.clusterHosts.contains(data.hostName)) {
            val hostState = ambariClient.getHostState(data.hostName)
            if ("UNKNOWN" == hostState) {
                deleteHosts(stack, asList(data.hostName), ArrayList(components))
                hostDeleted = true
            }
        } else {
            LOGGER.debug("Host is already deleted.")
            hostDeleted = true
        }
        return hostDeleted
    }

    private fun getHadoopComponents(ambariClient: AmbariClient, hostGroupName: String, blueprintName: String): Set<String> {
        return ambariClient.getComponentsCategory(blueprintName, hostGroupName).keys
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun deleteHosts(stack: Stack, hosts: List<String>, components: List<String>) {
        val cluster = stack.cluster
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        val ambariClient = ambariClientProvider!!.getAmbariClient(clientConfig, stack.gatewayPort, cluster.userName, cluster.password)
        for (hostName in hosts) {
            ambariClient.deleteHostComponents(hostName, components)
            ambariClient.deleteHost(hostName)
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun collectDownscaleCandidates(stack: Stack, cluster: Cluster, hostGroupName: String, scalingAdjustment: Int?): List<HostMetadata> {
        val downScaleCandidates: List<HostMetadata>
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        val ambariClient = ambariClientProvider!!.getAmbariClient(clientConfig, stack.gatewayPort, cluster.userName, cluster.password)
        val replication = getReplicationFactor(ambariClient, hostGroupName)
        val hostGroup = hostGroupService!!.getByClusterIdAndName(cluster.id, hostGroupName)
        val hostsInHostGroup = hostGroup.hostMetadata
        val filteredHostList = hostFilterService!!.filterHostsForDecommission(cluster, hostsInHostGroup, hostGroupName)
        val reservedInstances = hostsInHostGroup.size - filteredHostList.size
        verifyNodeCount(replication, scalingAdjustment!!, filteredHostList, reservedInstances)
        if (doesHostGroupContainDataNode(ambariClient, cluster.blueprint.blueprintName, hostGroup.name)) {
            downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication, scalingAdjustment, filteredHostList)
        } else {
            downScaleCandidates = filteredHostList
        }
        return downScaleCandidates
    }

    private fun getReplicationFactor(ambariClient: AmbariClient, hostGroup: String): Int {
        try {
            val configuration = configurationService!!.getConfiguration(ambariClient, hostGroup)
            return Integer.parseInt(configuration[ConfigParam.DFS_REPLICATION.key()])
        } catch (e: ConnectException) {
            LOGGER.error("Cannot connect to Ambari to get the configuration", e)
            throw BadRequestException("Cannot connect to Ambari")
        }

    }

    private fun verifyNodeCount(replication: Int, scalingAdjustment: Int, filteredHostList: List<HostMetadata>, reservedInstances: Int) {
        val adjustment = Math.abs(scalingAdjustment)
        val hostSize = filteredHostList.size
        if (hostSize + reservedInstances - adjustment < replication || hostSize < adjustment) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize)
            throw BadRequestException("There is not enough node to downscale. " + "Check the replication factor and the ApplicationMaster occupation.")
        }
    }

    private fun doesHostGroupContainDataNode(client: AmbariClient, blueprint: String, hostGroup: String): Boolean {
        return client.getBlueprintMap(blueprint)[hostGroup].contains(DATANODE)
    }

    private fun checkAndSortByAvailableSpace(stack: Stack, client: AmbariClient, replication: Int, adjustment: Int,
                                             filteredHostList: List<HostMetadata>): List<HostMetadata> {
        val removeCount = Math.abs(adjustment)
        val dfsSpace = getDFSSpace(stack, client)
        val sortedAscending = sortByUsedSpace(dfsSpace, false)
        val selectedNodes = selectNodes(sortedAscending, filteredHostList, removeCount)
        val remainingNodes = removeSelected(sortedAscending, selectedNodes)
        LOGGER.info("Selected nodes for decommission: {}", selectedNodes)
        LOGGER.info("Remaining nodes after decommission: {}", remainingNodes)
        val usedSpace = getSelectedUsage(selectedNodes)
        val remainingSpace = getRemainingSpace(remainingNodes, dfsSpace)
        val safetyUsedSpace = (usedSpace.toDouble() * replication.toDouble() * SAFETY_PERCENTAGE).toDouble().toLong()
        LOGGER.info("Checking DFS space for decommission, usedSpace: {}, remainingSpace: {}", usedSpace, remainingSpace)
        LOGGER.info("Used space with replication: {} and safety space: {} is: {}", replication, SAFETY_PERCENTAGE, safetyUsedSpace)
        if (remainingSpace < safetyUsedSpace) {
            throw BadRequestException(
                    String.format("Trying to move '%s' bytes worth of data to nodes with '%s' bytes of capacity is not allowed", usedSpace, remainingSpace))
        }
        return convert(selectedNodes, filteredHostList)
    }

    private fun getDFSSpace(stack: Stack, client: AmbariClient): Map<String, Map<Long, Long>> {
        val dfsSpaceTask = AmbariDFSSpaceRetrievalTask()
        val result = ambariClientPollingService!!.pollWithTimeoutSingleFailure(dfsSpaceTask, AmbariClientPollerObject(stack, client),
                AmbariDFSSpaceRetrievalTask.AMBARI_RETRYING_INTERVAL, AmbariDFSSpaceRetrievalTask.AMBARI_RETRYING_COUNT)
        if (result == PollingResult.SUCCESS) {
            return dfsSpaceTask.dfsSpace
        } else {
            throw CloudbreakServiceException("Failed to get dfs space from ambari!")

        }
    }

    private fun selectNodes(sortedAscending: Map<String, Long>, filteredHostList: List<HostMetadata>, removeCount: Int): Map<String, Long> {
        val select = HashMap<String, Long>()
        var i = 0
        for (host in sortedAscending.keys) {
            if (i < removeCount) {
                for (hostMetadata in filteredHostList) {
                    if (hostMetadata.hostName.equals(host, ignoreCase = true)) {
                        select.put(host, sortedAscending[host])
                        i++
                        break
                    }
                }
            } else {
                break
            }
        }
        return select
    }

    private fun removeSelected(all: Map<String, Long>, selected: Map<String, Long>): Map<String, Long> {
        val copy = HashMap(all)
        for (host in selected.keys) {
            val iterator = copy.keys.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().equals(host, ignoreCase = true)) {
                    iterator.remove()
                    break
                }
            }
        }
        return copy
    }

    private fun getSelectedUsage(selected: Map<String, Long>): Long {
        var usage: Long = 0
        for (host in selected.keys) {
            usage += selected[host]
        }
        return usage
    }

    private fun getRemainingSpace(remainingNodes: Map<String, Long>, dfsSpace: Map<String, Map<Long, Long>>): Long {
        var remaining: Long = 0
        for (host in remainingNodes.keys) {
            val space = dfsSpace[host]
            remaining += space.keys.iterator().next()
        }
        return remaining
    }

    private fun convert(selectedNodes: Map<String, Long>, filteredHostList: List<HostMetadata>): List<HostMetadata> {
        val result = ArrayList<HostMetadata>()
        for (host in selectedNodes.keys) {
            for (hostMetadata in filteredHostList) {
                if (hostMetadata.hostName.equals(host, ignoreCase = true)) {
                    result.add(hostMetadata)
                    break
                }
            }
        }
        return result
    }

    @Throws(CloudbreakException::class)
    private fun stopAndDeleteHosts(stack: Stack, ambariClient: AmbariClient, hostList: List<String>, components: Set<String>) {
        val orchestrator = stack.orchestrator
        val map = HashMap<String, Any>()
        map.putAll(orchestrator.attributes.map)
        map.put("certificateDir", tlsSecurityService!!.prepareCertDir(stack.id))
        val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator.type)
        try {
            if (orchestratorType.containerOrchestrator()) {
                val credential = OrchestrationCredential(orchestrator.apiEndpoint, map)
                val containerOrchestrator = containerOrchestratorResolver!!.get(orchestrator.type)
                val containers = containerRepository!!.findContainersInCluster(stack.cluster.id)

                val containersToDelete = containers.stream().filter({ input -> hostList.contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.name) }).map({ input -> ContainerInfo(input.getContainerId(), input.getName(), input.getHost(), input.getImage()) }).collect(Collectors.toList<ContainerInfo>())

                containerOrchestrator.deleteContainer(containersToDelete, credential)
                containerRepository.delete(containers)
                val pollingResult = waitForHostsToLeave(stack, ambariClient, hostList)
                if (isTimeout(pollingResult)) {
                    LOGGER.warn("Ambari agent stop timed out, delete the hosts anyway, hosts: {}", hostList)
                }
                if (!isExited(pollingResult)) {
                    deleteHosts(stack, hostList, components)
                }
            } else if (orchestratorType.hostOrchestrator()) {
                val hostOrchestrator = hostOrchestratorResolver!!.get(stack.orchestrator.type)
                val gateway = stack.gatewayInstanceGroup
                val gatewayInstance = gateway.instanceMetaData.iterator().next()
                val gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.id, gatewayInstance.publicIpWrapper,
                        stack.gatewayPort, gatewayInstance.privateIp, gatewayInstance.discoveryFQDN)
                hostOrchestrator.tearDown(gatewayConfig, hostList)
                deleteHosts(stack, hostList, components)
            }
        } catch (e: CloudbreakOrchestratorException) {
            LOGGER.error("Failed to delete containers while decommissioning: ", e)
            throw CloudbreakException("Failed to delete containers while decommissioning: ", e)
        }

    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun deleteHosts(stack: Stack, hostList: List<String>, components: Set<String>) {
        deleteHosts(stack, hostList, ArrayList(components))
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun waitForHostsToLeave(stack: Stack, ambariClient: AmbariClient, hostNames: List<String>): PollingResult {
        return ambariHostLeave!!.pollWithTimeout(hostsLeaveStatusCheckerTask, AmbariHostsWithNames(stack, ambariClient, hostNames),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS, AmbariOperationService.MAX_FAILURE_COUNT)
    }

    private fun waitForDataNodeDecommission(stack: Stack, ambariClient: AmbariClient): PollingResult {
        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.id)
        return ambariOperationService!!.waitForOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, emptyMap<String, Int>(),
                DECOMMISSION_SERVICES_AMBARI_PROGRESS_STATE)
    }

    private fun waitForRegionServerDecommission(stack: Stack, ambariClient: AmbariClient, hosts: List<String>, components: Set<String>): PollingResult {
        if (!components.contains("HBASE_REGIONSERVER")) {
            return SUCCESS
        }
        LOGGER.info("Waiting for RegionServers to move the regions to other servers")
        return rsPollerService!!.pollWithTimeoutSingleFailure(rsDecommissionStatusCheckerTask, AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_REGION_DECOM)
    }

    private fun selectHostsToRemove(decommissionCandidates: List<HostMetadata>, adjustment: Int): Map<String, HostMetadata> {
        val hostsToRemove = HashMap<String, HostMetadata>()
        var i = 0
        for (hostMetadata in decommissionCandidates) {
            val hostName = hostMetadata.hostName
            if (i < adjustment) {
                LOGGER.info("Host '{}' will be removed from Ambari cluster", hostName)
                hostsToRemove.put(hostName, hostMetadata)
            } else {
                break
            }
            i++
        }
        return hostsToRemove
    }

    private fun decommissionComponents(ambariClient: AmbariClient, hosts: List<String>, components: Set<String>): Map<String, Int> {
        val decommissionRequests = HashMap<String, Int>()
        if (components.contains("NODEMANAGER")) {
            val requestId = ambariClient.decommissionNodeManagers(hosts)
            decommissionRequests.put("NODEMANAGER_DECOMMISSION", requestId)
        }
        if (components.contains("DATANODE")) {
            val requestId = ambariClient.decommissionDataNodes(hosts)
            decommissionRequests.put("DATANODE_DECOMMISSION", requestId)
        }
        if (components.contains("HBASE_REGIONSERVER")) {
            ambariClient.setHBaseRegionServersToMaintenance(hosts, true)
            val requestId = ambariClient.decommissionHBaseRegionServers(hosts)
            decommissionRequests.put("HBASE_DECOMMISSION", requestId)
        }
        return decommissionRequests
    }

    private fun stopHadoopComponents(stack: Stack, ambariClient: AmbariClient, hosts: List<String>): PollingResult {
        try {
            val requestId = ambariClient.stopAllComponentsOnHosts(hosts)
            return ambariOperationService!!.waitForOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId),
                    STOP_SERVICES_AMBARI_PROGRESS_STATE)
        } catch (e: HttpResponseException) {
            val errorMessage = AmbariClientExceptionUtil.getErrorMessage(e)
            throw AmbariOperationFailedException("Ambari could not stop components. " + errorMessage, e)
        }

    }

    @Throws(CloudbreakException::class)
    private fun startServicesIfNeeded(stack: Stack, ambariClient: AmbariClient, blueprint: String): PollingResult {
        val stringIntegerMap = HashMap<String, Int>()
        val componentsCategory = ambariClient.getComponentsCategory(blueprint)
        val hostComponentsStates = ambariClient.hostComponentsStates
        val services = HashSet<String>()
        collectServicesToStart(componentsCategory, hostComponentsStates, services)
        if (!services.isEmpty()) {
            try {
                if (services.contains("HDFS")) {
                    val requestId = ambariClient.startService("HDFS")
                    stringIntegerMap.put("HDFS_START", requestId)
                }
                if (services.contains("HBASE")) {
                    val requestId = ambariClient.startService("HBASE")
                    stringIntegerMap.put("HBASE_START", requestId)
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to start HDFS/HBASE", e)
                throw BadRequestException("Failed to start the HDFS and HBASE services, it's possible that some of the nodes are unavailable")
            }

        }

        if (!stringIntegerMap.isEmpty()) {
            return ambariOperationService!!.waitForOperations(stack, ambariClient, stringIntegerMap, START_SERVICES_AMBARI_PROGRESS_STATE)
        } else {
            return SUCCESS
        }
    }

    private fun collectServicesToStart(componentsCategory: Map<String, String>, hostComponentsStates: Map<String, Map<String, String>>, services: MutableSet<String>) {
        for (hostComponentsEntry in hostComponentsStates.entries) {
            val componentStateMap = hostComponentsEntry.value
            for (componentStateEntry in componentStateMap.entries) {
                val componentKey = componentStateEntry.key
                val category = componentsCategory[componentKey]
                if ("CLIENT" != category) {
                    if ("STARTED" != componentStateEntry.value) {
                        if ("NODEMANAGER" == componentKey || "DATANODE" == componentKey) {
                            services.add("HDFS")
                        } else if ("HBASE_REGIONSERVER" == componentKey) {
                            services.add("HBASE")
                        } else {
                            LOGGER.info("No need to restart ambari service: {}", componentKey)
                        }
                    } else {
                        LOGGER.info("Ambari service already running: {}", componentKey)
                    }
                }
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariDecommissioner::class.java)

        private val MAX_ATTEMPTS_FOR_REGION_DECOM = 500
        private val DATANODE = "DATANODE"
        private val SAFETY_PERCENTAGE = 1.2
    }

}
