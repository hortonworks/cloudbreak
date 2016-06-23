package com.sequenceiq.cloudbreak.core.cluster

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner
import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Service
class AmbariClusterUpscaleService {

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val clusterService: ClusterService? = null

    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null

    @Inject
    private val containerRunner: ClusterContainerRunner? = null

    @Inject
    private val hostRunner: ClusterHostServiceRunner? = null

    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null

    @Inject
    private val instanceMetadataService: InstanceMetadataService? = null

    @Inject
    private val hostGroupService: HostGroupService? = null

    @Inject
    private val recipeEngine: RecipeEngine? = null

    @Throws(CloudbreakException::class)
    fun upscaleAmbari(stackId: Long?, hostGroupName: String, scalingAdjustment: Int?) {
        val stack = stackService!!.getById(stackId)
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Start adding cluster containers")
        val orchestrator = stack.orchestrator
        val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator.type)
        val hostsPerHostGroup = HashMap<String, List<String>>()
        if (orchestratorType.containerOrchestrator()) {
            val containers = containerRunner!!.addClusterContainers(stackId, hostGroupName, scalingAdjustment)
            for (containersEntry in containers.entries) {
                val hostNames = containersEntry.value.stream().map(Function<Container, String> { it.getHost() }).collect(Collectors.toList<String>())
                hostsPerHostGroup.put(containersEntry.key, hostNames)
            }
        } else if (orchestratorType.hostOrchestrator()) {
            val hosts = hostRunner!!.addAmbariServices(stackId, hostGroupName, scalingAdjustment)
            for (hostName in hosts.keys) {
                if (!hostsPerHostGroup.keys.contains(hostGroupName)) {
                    hostsPerHostGroup.put(hostGroupName, ArrayList<String>())
                }
                hostsPerHostGroup[hostGroupName].add(hostName)
            }
        } else {
            LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.type))
            throw CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.type))
        }
        clusterService!!.updateHostMetadata(stack.cluster.id, hostsPerHostGroup)
        val allHosts = HashSet<String>()
        for (hostsPerHostGroupEntry in hostsPerHostGroup.entries) {
            allHosts.addAll(hostsPerHostGroupEntry.value)
        }
        clusterService.updateHostCountWithAdjustment(stack.cluster.id, hostGroupName, allHosts.size)
        if ("BYOS" != stack.cloudPlatform()) {
            instanceMetadataService!!.updateInstanceStatus(stack.instanceGroups, InstanceStatus.UNREGISTERED, allHosts)
        }
        ambariClusterConnector!!.waitForAmbariHosts(stackService.getById(stackId))
    }

    @Throws(CloudbreakException::class)
    fun executePreRecipesOnNewHosts(stackId: Long?, hostGroupName: String) {
        val stack = stackService!!.getById(stackId)
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Start executing pre recipes")
        val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, hostGroupName)
        val hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.id)
        recipeEngine!!.executeUpscalePreInstall(stack, hostGroup, hostMetadata)
    }

    @Throws(CloudbreakException::class)
    fun installServicesOnNewHosts(stackId: Long?, hostGroupName: String) {
        val stack = stackService!!.getById(stackId)
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Start installing Ambari services")
        val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, hostGroupName)
        val hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.id)
        ambariClusterConnector!!.installServices(stack, hostGroup, hostMetadata)
    }

    @Throws(CloudbreakException::class)
    fun executePostRecipesOnNewHosts(stackId: Long?, hostGroupName: String) {
        val stack = stackService!!.getById(stackId)
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Start executing post recipes")
        val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, hostGroupName)
        val hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.id)
        recipeEngine!!.executeUpscalePostInstall(stack, hostMetadata)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AmbariClusterUpscaleService::class.java)
    }
}
