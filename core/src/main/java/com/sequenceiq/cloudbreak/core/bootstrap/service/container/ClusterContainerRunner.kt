package com.sequenceiq.cloudbreak.core.bootstrap.service.container

import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_DB
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_SERVER
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.CONSUL_WATCH
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.HAVEGED
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.KERBEROS
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.LDAP
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.LOGROTATE
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.REGISTRATOR
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.SHIPYARD
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.SHIPYARD_DB

import java.util.ArrayList
import java.util.HashMap
import java.util.stream.Collectors

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerConfigService
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Container
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.ContainerService

@Component
class ClusterContainerRunner {

    @Inject
    private val clusterService: ClusterService? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val hostGroupRepository: HostGroupRepository? = null

    @Inject
    private val containerConfigService: ContainerConfigService? = null

    @Inject
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null

    @Inject
    private val containerService: ContainerService? = null

    @Inject
    private val conversionService: ConversionService? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Inject
    private val constraintFactory: ContainerConstraintFactory? = null

    @Throws(CloudbreakException::class)
    fun runClusterContainers(stack: Stack): Map<String, List<Container>> {
        try {
            val cloudPlatform = if (StringUtils.isNotEmpty(stack.cloudPlatform())) stack.cloudPlatform() else NONE
            return initializeClusterContainers(stack, cloudPlatform)
        } catch (e: CloudbreakOrchestratorCancelledException) {
            throw CancellationException(e.message)
        } catch (e: CloudbreakOrchestratorException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class)
    fun addClusterContainers(stackId: Long?, hostGroupName: String, scalingAdjustment: Int?): Map<String, List<Container>> {
        try {
            val stack = stackRepository!!.findOneWithLists(stackId)
            val cloudPlatform = if (StringUtils.isNotEmpty(stack.cloudPlatform())) stack.cloudPlatform() else NONE
            return addClusterContainers(stack, cloudPlatform, hostGroupName, scalingAdjustment)
        } catch (e: CloudbreakOrchestratorCancelledException) {
            throw CancellationException(e.message)
        } catch (e: CloudbreakOrchestratorException) {
            throw CloudbreakException(e)
        }

    }

    @Throws(CloudbreakException::class, CloudbreakOrchestratorException::class)
    private fun initializeClusterContainers(stack: Stack, cloudPlatform: String): Map<String, List<Container>> {

        val orchestrator = stack.orchestrator
        val map = HashMap<String, Any>()
        map.putAll(orchestrator.attributes.map)
        map.put("certificateDir", tlsSecurityService!!.prepareCertDir(stack.id))
        val credential = OrchestrationCredential(orchestrator.apiEndpoint, map)
        val containerOrchestrator = containerOrchestratorResolver!!.get(orchestrator.type)
        val containers = HashMap<String, List<ContainerInfo>>()
        val cluster = clusterService!!.retrieveClusterByStackId(stack.id)

        val gatewayHostname = getGatewayHostName(stack)

        try {
            if (SWARM == orchestrator.type) {
                val registratorConstraint = constraintFactory!!.getRegistratorConstraint(gatewayHostname, cluster.name,
                        getGatewayPrivateIp(stack))
                containers.put(REGISTRATOR.name, containerOrchestrator.runContainer(containerConfigService!!.get(stack, REGISTRATOR), credential,
                        registratorConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))
            }

            val ambariServerDbConstraint = constraintFactory!!.getAmbariServerDbConstraint(gatewayHostname, cluster.name)
            val dbContainer = containerOrchestrator.runContainer(containerConfigService!!.get(stack, AMBARI_DB), credential,
                    ambariServerDbConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id))
            containers.put(AMBARI_DB.name, dbContainer)

            val serverDbHostName = dbContainer[0].host
            val ambariServerConstraint = constraintFactory.getAmbariServerConstraint(serverDbHostName, gatewayHostname,
                    cloudPlatform, cluster.name)
            val ambariServerContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_SERVER),
                    credential, ambariServerConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id))
            containers.put(AMBARI_SERVER.name, ambariServerContainer)
            val ambariServerHost = ambariServerContainer[0].host

            if (cluster.isSecure) {
                val havegedConstraint = constraintFactory.getHavegedConstraint(gatewayHostname, cluster.name)
                containers.put(HAVEGED.name, containerOrchestrator.runContainer(containerConfigService.get(stack, HAVEGED), credential, havegedConstraint,
                        clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))

                val kerberosServerConstraint = constraintFactory.getKerberosServerConstraint(cluster, gatewayHostname)
                containers.put(KERBEROS.name, containerOrchestrator.runContainer(containerConfigService.get(stack, KERBEROS), credential,
                        kerberosServerConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))
            }

            if (cluster.isLdapRequired!!) {
                val ldapConstraint = constraintFactory.getLdapConstraint(ambariServerHost)
                containers.put(LDAP.name, containerOrchestrator.runContainer(containerConfigService.get(stack, LDAP), credential, ldapConstraint,
                        clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))
            }

            if (SWARM == orchestrator.type && cluster.enableShipyard!!) {
                val shipyardDbConstraint = constraintFactory.getShipyardDbConstraint(ambariServerHost)
                containers.put(SHIPYARD_DB.name, containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD_DB), credential,
                        shipyardDbConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))

                val shipyardConstraint = constraintFactory.getShipyardConstraint(ambariServerHost)
                containers.put(SHIPYARD.name, containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD), credential, shipyardConstraint,
                        clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))
            }

            val hostBlackList = ArrayList<String>()
            for (hostGroup in hostGroupRepository!!.findHostGroupsInCluster(stack.cluster.id)) {
                val ambariAgentConstraint = constraintFactory.getAmbariAgentConstraint(ambariServerHost, null, cloudPlatform, hostGroup,
                        null, hostBlackList)
                val containerInfos = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                        ambariAgentConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id))
                containers.put(hostGroup.name, containerInfos)
                hostBlackList.addAll(getHostsFromContainerInfo(containerInfos))
            }

            if (SWARM == orchestrator.type) {
                val hosts = getHosts(stack)
                val consulWatchConstraint = constraintFactory.getConsulWatchConstraint(hosts)
                containers.put(CONSUL_WATCH.name, containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential,
                        consulWatchConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))

                val logrotateConstraint = constraintFactory.getLogrotateConstraint(hosts)
                containers.put(LOGROTATE.name, containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential,
                        logrotateConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))
            }
            return saveContainers(containers, cluster)
        } catch (ex: CloudbreakOrchestratorException) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster)
            }
            checkCancellation(ex)
            throw ex
        }

    }

    private fun getGatewayHostName(stack: Stack): String {
        var gatewayHostname = ""
        if (stack.instanceGroups != null && !stack.instanceGroups.isEmpty()) {
            val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
            gatewayHostname = gatewayInstance.discoveryFQDN
        }
        return gatewayHostname
    }

    private fun getGatewayPrivateIp(stack: Stack): String {
        var gatewayHostname = ""
        if (stack.instanceGroups != null && !stack.instanceGroups.isEmpty()) {
            val gatewayInstance = stack.gatewayInstanceGroup.instanceMetaData.iterator().next()
            gatewayHostname = gatewayInstance.privateIp
        }
        return gatewayHostname
    }

    @Throws(CloudbreakException::class, CloudbreakOrchestratorException::class)
    private fun addClusterContainers(stack: Stack, cloudPlatform: String, hostGroupName: String, adjustment: Int?): Map<String, List<Container>> {

        val orchestrator = stack.orchestrator
        val map = HashMap<String, Any>()
        map.putAll(orchestrator.attributes.map)
        map.put("certificateDir", tlsSecurityService!!.prepareCertDir(stack.id))
        val credential = OrchestrationCredential(orchestrator.apiEndpoint, map)
        val containerOrchestrator = containerOrchestratorResolver!!.get(orchestrator.type)
        val containers = HashMap<String, List<ContainerInfo>>()
        val cluster = clusterService!!.retrieveClusterByStackId(stack.id)

        try {
            val existingContainers = containerService!!.findContainersInCluster(cluster.id)
            val ambariServerHost = existingContainers.stream().filter({ input -> input.getImage().contains(AMBARI_SERVER.name) }).findFirst().get().getHost()
            val hostGroup = hostGroupRepository!!.findHostGroupInClusterByName(cluster.id, hostGroupName)
            val ambariAgentApp = existingContainers.stream().filter({ input -> hostGroup.hostNames.contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.name) }).findFirst().get().getName()
            val hostBlackList = getOtherHostgroupsAgentHostsFromContainer(existingContainers, hostGroupName)
            val ambariAgentConstraint = constraintFactory!!.getAmbariAgentConstraint(ambariServerHost, ambariAgentApp,
                    cloudPlatform, hostGroup, adjustment, hostBlackList)
            containers.put(hostGroup.name, containerOrchestrator.runContainer(containerConfigService!!.get(stack, AMBARI_AGENT), credential,
                    ambariAgentConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))

            if (SWARM == orchestrator.type) {
                val hosts = ambariAgentConstraint.hosts

                val consulWatchConstraint = constraintFactory.getConsulWatchConstraint(hosts)
                containers.put(CONSUL_WATCH.name, containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential,
                        consulWatchConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))

                val logrotateConstraint = constraintFactory.getLogrotateConstraint(hosts)
                containers.put(LOGROTATE.name, containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential,
                        logrotateConstraint, clusterDeletionBasedExitCriteriaModel(stack.id, cluster.id)))
            }

            return saveContainers(containers, cluster)
        } catch (ex: CloudbreakOrchestratorException) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster)
            }
            checkCancellation(ex)
            throw ex
        }

    }

    private fun getOtherHostgroupsAgentHostsFromContainer(existingContainers: Set<Container>, hostGroupName: String): List<String> {
        val hostGroupNamePart = hostGroupName.replace("_", "-")
        return existingContainers.stream().filter({ input -> input.getImage().contains(AMBARI_AGENT.name) && !input.getName().contains(hostGroupNamePart) }).map(Function<Container, String> { it.getHost() }).collect(Collectors.toList<String>())
    }

    private fun getHostsFromContainerInfo(containerInfos: List<ContainerInfo>): List<String> {
        return containerInfos.stream().map(Function<ContainerInfo, String> { it.getHost() }).collect(Collectors.toList<String>())
    }

    private fun getHosts(stack: Stack): List<String> {
        val hosts = ArrayList<String>()
        for (instanceMetaData in stack.runningInstanceMetaData) {
            hosts.add(instanceMetaData.discoveryFQDN)
        }
        return hosts
    }

    private fun convert(containerInfo: List<ContainerInfo>, cluster: Cluster): List<Container> {
        val containers = ArrayList<Container>()
        for (source in containerInfo) {
            val container = conversionService!!.convert<Container>(source, Container::class.java)
            container.cluster = cluster
            containers.add(container)
        }
        return containers
    }

    private fun saveContainers(containerInfo: Map<String, List<ContainerInfo>>, cluster: Cluster): Map<String, List<Container>> {
        val containers = HashMap<String, List<Container>>()
        for (containerInfoEntry in containerInfo.entries) {
            val hostGroupContainers = convert(containerInfoEntry.value, cluster)
            containers.put(containerInfoEntry.key, hostGroupContainers)
            containerService!!.save(hostGroupContainers)
        }
        return containers
    }

    private fun checkCancellation(ex: CloudbreakOrchestratorException) {
        if (ex is CloudbreakOrchestratorCancelledException || ExceptionUtils.getRootCause(ex) is CloudbreakOrchestratorCancelledException) {
            throw CancellationException("Creation of cluster containers was cancelled.")
        }
    }

    companion object {

        private val NONE = "none"
    }
}

