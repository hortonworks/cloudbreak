package com.sequenceiq.cloudbreak.orchestrator.swarm


import com.github.dockerjava.api.model.RestartPolicy.alwaysRestart
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM
import java.lang.String.format

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.concurrent.Callable
import java.util.concurrent.Future

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Link
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner
import com.sequenceiq.cloudbreak.orchestrator.container.SimpleContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.SwarmOrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.SwarmOrchestratorDeletion

@Component
class SwarmContainerOrchestrator : SimpleContainerOrchestrator() {


    /**
     * Bootstraps a Swarm based container orchestration cluster with a Consul discovery backend with the Munchausen tool.

     * @param gatewayConfig     Config used to access the gateway instance
     * *
     * @param nodes             Nodes that must be added to the Swarm cluster
     * *
     * @param consulServerCount Number of Consul servers in the cluster
     * *
     * @return The API address of the container orchestrator
     */
    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int,
                           exitCriteriaModel: ExitCriteriaModel) {
        try {
            val privateGatewayIp = getPrivateGatewayIp(gatewayConfig.publicAddress, nodes)
            val privateAddresses = getPrivateAddresses(nodes)
            val privateAddressesWithoutGateway = getPrivateAddresses(getNodesWithoutGateway(gatewayConfig.publicAddress, nodes))
            val consulServers = selectConsulServers(privateGatewayIp, privateAddressesWithoutGateway, consulServerCount)
            val result = prepareDockerAddressInventory(privateAddresses)

            val cmd = arrayOf("--debug", "bootstrap", "--wait", MUNCHAUSEN_WAIT, "--consulServers", concatToString(consulServers), concatToString(result))

            runner(munchausenBootstrap(gatewayConfig, imageName(config), cmd),
                    exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap()).call()

        } catch (coe: CloudbreakOrchestratorCancelledException) {
            throw coe
        } catch (coe: CloudbreakOrchestratorFailedException) {
            throw coe
        } catch (ex: Exception) {
            throw CloudbreakOrchestratorFailedException(ex)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>,
                                   exitCriteriaModel: ExitCriteriaModel) {
        try {
            val privateAddresses = getPrivateAddresses(nodes)
            val result = prepareDockerAddressInventory(privateAddresses)
            val cmd = arrayOf("--debug", "add", "--wait", MUNCHAUSEN_WAIT, "--join", getConsulJoinIp(gatewayConfig.privateAddress), concatToString(result))

            runner(munchausenNewNodeBootstrap(gatewayConfig, imageName(config), cmd),
                    exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap()).call()

        } catch (coe: CloudbreakOrchestratorCancelledException) {
            throw coe
        } catch (coe: CloudbreakOrchestratorFailedException) {
            throw coe
        } catch (ex: Exception) {
            throw CloudbreakOrchestratorFailedException(ex)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun validateApiEndpoint(cred: OrchestrationCredential) {
        // TODO
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun runContainer(config: ContainerConfig, cred: OrchestrationCredential, constraint: ContainerConstraint,
                              exitCriteriaModel: ExitCriteriaModel): List<ContainerInfo> {
        val containerInfos = ArrayList<ContainerInfo>()
        val image = imageName(config)
        try {
            val futures = ArrayList<Future<Boolean>>()
            var i = 0
            for (fqdn in constraint.getHosts()) {
                val nodeName = fqdn.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
                val dockerApiClient = swarmClient(cred)
                val name = createSwarmContainerName(constraint, i++)
                val createCmd = decorateCreateContainerCmd(image, constraint, nodeName, dockerApiClient, name)
                val bootstrap = SwarmOrchestratorBootstrap(dockerApiClient, nodeName, createCmd)
                val runner = runner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap())
                futures.add(parallelOrchestratorComponentRunner!!.submit(runner))
                containerInfos.add(ContainerInfo(name, name, fqdn, image))
            }
            for (future in futures) {
                future.get()
            }
            return containerInfos
        } catch (ex: Exception) {
            deleteContainer(containerInfos, cred)
            throw CloudbreakOrchestratorFailedException(ex)
        }

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun startContainer(info: List<ContainerInfo>, cred: OrchestrationCredential) {

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun stopContainer(info: List<ContainerInfo>, cred: OrchestrationCredential) {

    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun deleteContainer(containerInfo: List<ContainerInfo>, cred: OrchestrationCredential) {
        try {
            val dockerApiClient = swarmClient(cred)
            val futures = ArrayList<Future<Boolean>>()
            for (info in containerInfo) {
                try {
                    val hostName = info.host.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
                    val containerRemover = SwarmOrchestratorDeletion(dockerApiClient, hostName, info.name)
                    val runner = runner(containerRemover, exitCriteria, null, MDC.getCopyOfContextMap())
                    futures.add(parallelOrchestratorComponentRunner!!.submit(runner))
                } catch (me: Exception) {
                    throw CloudbreakOrchestratorFailedException(me)
                }

            }

            for (future in futures) {
                future.get()
            }
        } catch (ex: Exception) {
            val msg = String.format("Failed to delete containers: '%s'.", Arrays.toString(containerInfo.toArray<String>(arrayOfNulls<String>(containerInfo.size))))
            throw CloudbreakOrchestratorFailedException(msg, ex)
        }

    }

    override fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        val missingNodes = getPrivateAddresses(nodes)
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.")
        try {
            val allAvailableNodes = getAvailableNodes(gatewayConfig, nodes)
            LOGGER.info("Available swarm nodes: {}/{}", allAvailableNodes.size, missingNodes.size)
            for (availableNode in allAvailableNodes) {
                missingNodes.remove(availableNode)
            }

        } catch (t: Exception) {
            LOGGER.info(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", t.message))
        }

        return Lists.newArrayList(missingNodes)
    }

    override fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.")
        val privateAddresses = ArrayList<String>()
        try {
            val swarmClientConfig = getSwarmClientConfig(gatewayConfig.gatewayUrl, gatewayConfig.certificateDir)
            val swarmManagerClient = DockerClientBuilder.getInstance(swarmClientConfig).withDockerCmdExecFactory(DockerCmdExecFactoryImpl()).build()
            val driverStatus = swarmManagerClient.infoCmd().exec().driverStatuses
            LOGGER.debug("Swarm manager is available, checking registered agents.")
            for (element in driverStatus) {
                try {
                    val objects = element as ArrayList<Any>
                    for (node in nodes) {
                        if ((objects[1] as String).split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0] == node.privateIp) {
                            privateAddresses.add(node.privateIp)
                            break
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.warn(String.format("Docker info returned an unexpected element: %s", element), e)
                }

            }
            return privateAddresses
        } catch (e: Exception) {
            val defaultErrorMessage = "502 Bad Gateway"
            val errorMessage = if (e.message.contains(defaultErrorMessage)) defaultErrorMessage else e.message
            LOGGER.warn(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", errorMessage))
            return privateAddresses
        }

    }

    override fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean {
        LOGGER.info("Checking if docker daemon is available.")
        try {
            val dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig)).withDockerCmdExecFactory(DockerCmdExecFactoryImpl()).build()
            dockerApiClient.infoCmd().exec()
            return true
        } catch (ex: Exception) {
            LOGGER.warn(String.format("Docker api not available: %s", ex.message))
            return false
        }

    }

    override val maxBootstrapNodes: Int
        get() = MAX_IP_FOR_ONE_REQUEST

    override fun name(): String {
        return SWARM
    }

    private fun createSwarmContainerName(constraint: ContainerConstraint, index: Int): String {
        var name = constraint.containerName.getName()
        if (constraint.getHosts().size > 1) {
            name = String.format("%s-%s", name, index)
        }
        return name
    }

    private fun decorateCreateContainerCmd(image: String, constraint: ContainerConstraint, hostname: String,
                                           dockerApiClient: DockerClient, name: String): CreateContainerCmd {
        val env = createEnv(constraint, hostname)
        val cmd = constraint.cmd
        val createCmd = dockerApiClient.createContainerCmd(image).withName(name).withRestartPolicy(alwaysRestart()).withPrivileged(true).withEnv(*env)

        if (cmd != null && cmd.size > 0) {
            createCmd.withCmd(*cmd)
        }

        if (!StringUtils.isEmpty(constraint.networkMode)) {
            createCmd.withNetworkMode(constraint.networkMode)
        }

        val binds = createVolumeBinds(constraint)
        if (binds.size > 0) {
            createCmd.withBinds(*binds)
        }

        val portBinding = constraint.tcpPortBinding
        if (portBinding != null) {
            val ports = Ports(ExposedPort.tcp(portBinding.exposedPort!!), Ports.Binding(portBinding.hostIp, portBinding.hostPort))
            createCmd.withPortBindings(ports)
        }

        val links = ArrayList<Link>()
        for (entry in constraint.getLinks().entries) {
            val link = Link(entry.key, entry.value)
            links.add(link)
        }
        createCmd.withLinks(*links.toArray<Link>(arrayOfNulls<Link>(links.size)))

        return createCmd
    }

    private fun createEnv(constraint: ContainerConstraint, hostname: String): Array<String> {
        val env = ArrayList<String>()
        for (envEntry in constraint.getEnv().entries) {
            val envVariable = envEntry.key + ENV_KEY_VALUE_SEPARATOR + envEntry.value
            env.add(envVariable)
        }
        env.add(format("constraint:node==%s", hostname))
        val result = arrayOfNulls<String>(env.size)
        return env.toArray<String>(result)
    }

    private fun createVolumeBinds(constraint: ContainerConstraint): Array<Bind> {
        val bindsBuilder = BindsBuilder()
        for ((hostPath, containerPath) in constraint.getVolumeBinds()) {
            if (StringUtils.isEmpty(containerPath)) {
                bindsBuilder.add(hostPath)
            } else {
                bindsBuilder.add(hostPath, containerPath)
            }
        }
        return bindsBuilder.build()
    }

    private fun selectConsulServers(gatewayAddress: String, privateAddresses: Set<String>, consulServerCount: Int): Set<String> {
        val privateAddressList = ArrayList(privateAddresses)
        val consulServers = if (consulServerCount <= privateAddressList.size + 1) consulServerCount else privateAddressList.size
        val result = HashSet<String>()
        result.add(gatewayAddress)
        for (i in 0..consulServers - 1 - 1) {
            result.add(privateAddressList[i])
        }
        return result
    }

    private fun concatToString(items: Collection<String>): String {
        val sb = StringBuilder()
        for (item in items) {
            sb.append(item + ",")
        }
        return sb.toString().substring(0, sb.toString().length - 1)
    }

    private fun getPrivateAddresses(nodes: Collection<Node>): MutableSet<String> {
        val privateAddresses = HashSet<String>()
        for (node in nodes) {
            privateAddresses.add(node.privateIp)
        }
        return privateAddresses
    }

    private fun getPrivateGatewayIp(gatewayAddress: String, nodes: Collection<Node>): String? {
        for (node in nodes) {
            if (node.publicIp != null && node.publicIp == gatewayAddress) {
                return node.privateIp
            }
        }
        return null
    }

    private fun getNodesWithoutGateway(gatewayAddress: String, nodes: Collection<Node>): Set<Node> {
        val coreNodes = HashSet<Node>()
        for (node in nodes) {
            if (node.publicIp == null || node.publicIp != gatewayAddress) {
                coreNodes.add(node)
            }
        }
        return coreNodes
    }

    @VisibleForTesting
    internal fun prepareDockerAddressInventory(nodeAddresses: Collection<String>): Set<String> {
        val nodeResult = HashSet<String>()
        for (nodeAddress in nodeAddresses) {
            nodeResult.add(String.format("%s:2376", nodeAddress))
        }
        return nodeResult
    }

    private fun getSwarmClientConfig(gatewayUrl: String, certificateDir: String): DockerClientConfig {
        return DockerClientConfig.createDefaultConfigBuilder().withDockerCertPath(certificateDir).withVersion("1.18").withUri(gatewayUrl + "/swarm").build()
    }

    private fun getDockerClientConfig(gatewayConfig: GatewayConfig): DockerClientConfig {
        return DockerClientConfig.createDefaultConfigBuilder().withDockerCertPath(gatewayConfig.certificateDir).withVersion("1.18").withUri(gatewayConfig.gatewayUrl + "/docker").build()
    }

    @VisibleForTesting
    internal fun dockerClient(gatewayConfig: GatewayConfig): DockerClient {
        return DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig)).withDockerCmdExecFactory(DockerCmdExecFactoryImpl().withReadTimeout(READ_TIMEOUT)).build()
    }

    @VisibleForTesting
    internal fun swarmClient(gatewayConfig: GatewayConfig): DockerClient {
        return DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayConfig.gatewayUrl, gatewayConfig.certificateDir)).withDockerCmdExecFactory(DockerCmdExecFactoryImpl().withReadTimeout(READ_TIMEOUT)).build()
    }

    internal fun swarmClient(cred: OrchestrationCredential): DockerClient {
        val gatewayUrl = "https://" + cred.apiEndpoint
        return DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayUrl, cred.properties["certificateDir"] as String)).withDockerCmdExecFactory(DockerCmdExecFactoryImpl().withReadTimeout(READ_TIMEOUT)).build()
    }

    @VisibleForTesting
    internal fun munchausenBootstrap(gatewayConfig: GatewayConfig, imageName: String, cmd: Array<String>): MunchausenBootstrap {
        val dockerApiClient = dockerClient(gatewayConfig)
        return MunchausenBootstrap(dockerApiClient, imageName, cmd)
    }

    @VisibleForTesting
    internal fun munchausenNewNodeBootstrap(gatewayConfig: GatewayConfig, imageName: String, cmd: Array<String>): MunchausenBootstrap {
        val dockerApiClient = swarmClient(gatewayConfig)
        return MunchausenBootstrap(dockerApiClient, imageName, cmd)
    }

    @VisibleForTesting
    fun runner(bootstrap: OrchestratorBootstrap, exitCriteria: ExitCriteria, exitCriteriaModel: ExitCriteriaModel?,
               mdcMap: Map<String, String>): Callable<Boolean> {
        return OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, mdcMap)
    }

    private fun getConsulJoinIp(privateIp: String): String {
        return String.format("consul://%s:8500", privateIp)
    }

    private fun imageName(containerConfig: ContainerConfig): String {
        return containerConfig.name + ":" + containerConfig.version
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SwarmContainerOrchestrator::class.java)
        private val READ_TIMEOUT = 180000
        private val MUNCHAUSEN_WAIT = "3600"
        private val MAX_IP_FOR_ONE_REQUEST = 600
        private val ENV_KEY_VALUE_SEPARATOR = "="
    }
}
