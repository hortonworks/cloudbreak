package com.sequenceiq.cloudbreak.core.bootstrap.service

import java.util.ArrayList
import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

@Component
class ClusterServiceRunner {

    @Inject
    private val stackService: StackService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null
    @Inject
    private val instanceMetadataService: InstanceMetadataService? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val containerRunner: ClusterContainerRunner? = null
    @Inject
    private val hostRunner: ClusterHostServiceRunner? = null

    @Throws(CloudbreakException::class)
    fun runAmbariServices(stackId: Long?) {
        val stack = stackService!!.getById(stackId)
        val orchestrator = stack.orchestrator
        val cluster = clusterService!!.retrieveClusterByStackId(stack.id)
        MDCBuilder.buildMdcContext(cluster)
        val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator.type)
        if (orchestratorType.containerOrchestrator()) {
            val containers = containerRunner!!.runClusterContainers(stack)
            val ambariClientConfig = buildAmbariClientConfig(stack)
            clusterService.updateAmbariClientConfig(cluster.id, ambariClientConfig)
            val hostsPerHostGroup = HashMap<String, List<String>>()
            for (containersEntry in containers.entries) {
                val hostNames = ArrayList<String>()
                for (container in containersEntry.value) {
                    hostNames.add(container.host)
                }
                hostsPerHostGroup.put(containersEntry.key, hostNames)
            }
            clusterService.updateHostMetadata(cluster.id, hostsPerHostGroup)
        } else if (orchestratorType.hostOrchestrator()) {
            hostRunner!!.runAmbariServices(stack)
            val ambariClientConfig = buildAmbariClientConfig(stack)
            clusterService.updateAmbariClientConfig(cluster.id, ambariClientConfig)
            val hostsPerHostGroup = HashMap<String, List<String>>()
            for (instanceMetaData in stack.runningInstanceMetaData) {
                val groupName = instanceMetaData.instanceGroup.groupName
                if (!hostsPerHostGroup.keys.contains(groupName)) {
                    hostsPerHostGroup.put(groupName, ArrayList<String>())
                }
                hostsPerHostGroup[groupName].add(instanceMetaData.discoveryFQDN)
            }
            clusterService.updateHostMetadata(cluster.id, hostsPerHostGroup)
        } else {
            LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.type))
            throw CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.type))
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun buildAmbariClientConfig(stack: Stack): HttpClientConfig {
        val newStatusByGroupType = HashMap<InstanceGroupType, InstanceStatus>()
        newStatusByGroupType.put(InstanceGroupType.GATEWAY, InstanceStatus.REGISTERED)
        newStatusByGroupType.put(InstanceGroupType.CORE, InstanceStatus.UNREGISTERED)
        instanceMetadataService!!.updateInstanceStatus(stack.instanceGroups, newStatusByGroupType)
        val gateway = stack.gatewayInstanceGroup
        val gatewayInstance = gateway.instanceMetaData.iterator().next()
        return tlsSecurityService!!.buildTLSClientConfig(stack.id, gatewayInstance.publicIpWrapper)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterServiceRunner::class.java)
    }
}
