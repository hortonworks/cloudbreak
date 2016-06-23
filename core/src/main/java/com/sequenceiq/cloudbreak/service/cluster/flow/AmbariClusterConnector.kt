package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.service.PollingResult.isExited
import com.sequenceiq.cloudbreak.service.PollingResult.isFailure
import com.sequenceiq.cloudbreak.service.PollingResult.isSuccess
import com.sequenceiq.cloudbreak.service.PollingResult.isTimeout
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_AMBARI_SERVER_STARTUP
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.INSTALL_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.SMOKE_TEST_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_OPERATION_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.STOP_AMBARI_PROGRESS_STATE
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE
import java.util.Collections.singletonMap

import java.io.IOException
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors

import javax.annotation.Resource
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.ambari.client.AmbariConnectionException
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration
import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.common.type.HostMetadataState
import com.sequenceiq.cloudbreak.common.type.ResourceType
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.ClusterException
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.FileSystem
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.RDSConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Topology
import com.sequenceiq.cloudbreak.domain.TopologyRecord
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.PollingResult
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.RDSConfigProvider
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.SmartSenseConfigProvider
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.image.ImageService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListenerTask
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupPollerObject
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil
import com.sequenceiq.cloudbreak.util.JsonUtil

import groovyx.net.http.HttpResponseException

@Service
class AmbariClusterConnector {

    @Inject
    private val stackRepository: StackRepository? = null
    @Inject
    private val clusterRepository: ClusterRepository? = null
    @Inject
    private val instanceMetadataRepository: InstanceMetaDataRepository? = null
    @Inject
    private val hostGroupService: HostGroupService? = null
    @Inject
    private val ambariOperationService: AmbariOperationService? = null
    @Inject
    private val hostsPollingService: PollingService<AmbariHostsCheckerContext>? = null
    @Inject
    private val hadoopConfigurationService: HadoopConfigurationService? = null
    @Inject
    private val ambariClientProvider: AmbariClientProvider? = null
    @Inject
    private val eventService: CloudbreakEventService? = null
    @Inject
    private val recipeEngine: RecipeEngine? = null
    @Inject
    private val ambariHostsStatusCheckerTask: AmbariHostsStatusCheckerTask? = null
    @Inject
    private val ambariHostJoin: PollingService<AmbariHostsCheckerContext>? = null
    @Inject
    private val ambariHealthChecker: PollingService<AmbariClientPollerObject>? = null
    @Inject
    private val ambariStartupPollerObjectPollingService: PollingService<AmbariStartupPollerObject>? = null
    @Inject
    private val ambariStartupListenerTask: AmbariStartupListenerTask? = null
    @Inject
    private val ambariHealthCheckerTask: AmbariHealthCheckerTask? = null
    @Inject
    private val ambariHostsJoinStatusCheckerTask: AmbariHostsJoinStatusCheckerTask? = null
    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null
    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null
    @Resource
    private val fileSystemConfigurators: Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>>? = null
    @Inject
    private val blueprintProcessor: BlueprintProcessor? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val smartSenseConfigProvider: SmartSenseConfigProvider? = null
    @Inject
    private val rdsConfigProvider: RDSConfigProvider? = null
    @Inject
    private val imageService: ImageService? = null

    @Throws(CloudbreakException::class)
    fun waitForAmbariServer(stack: Stack) {
        val ambariClient = getDefaultAmbariClient(stack)
        val ambariStartupPollerObject = AmbariStartupPollerObject(stack, stack.ambariIp, ambariClient)
        val pollingResult = ambariStartupPollerObjectPollingService!!.pollWithTimeoutSingleFailure(ambariStartupListenerTask, ambariStartupPollerObject,
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_SERVER_STARTUP)
        if (isSuccess(pollingResult)) {
            LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult)
        } else if (isExited(pollingResult)) {
            throw CancellationException("Polling of Ambari server start has been cancelled.")
        } else {
            LOGGER.info("Could not start Ambari. polling result: {}", pollingResult)
            throw CloudbreakException(String.format("Could not start Ambari. polling result: '%s'", pollingResult))
        }
    }

    fun buildAmbariCluster(stack: Stack): Cluster {
        var cluster = stack.cluster
        try {
            cluster.creationStarted = Date().time
            cluster = clusterRepository!!.save(cluster)

            var blueprintText = cluster.blueprint.blueprintText
            val fs = cluster.fileSystem

            blueprintText = updateBlueprintConfiguration(stack, blueprintText, cluster.rdsConfig, fs)

            val ambariClient = getAmbariClient(stack)
            setBaseRepoURL(stack, ambariClient)
            addBlueprint(stack, ambariClient, blueprintText)
            val hostGroups = hostGroupService!!.getByCluster(cluster.id)
            val hostGroupMappings = buildHostGroupAssociations(hostGroups)

            val hostsInCluster = hostMetadataRepository!!.findHostsInCluster(cluster.id)
            val waitForHostsResult = waitForHosts(stack, ambariClient, hostsInCluster)
            checkPollingResult(waitForHostsResult, cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_HOST_JOIN_FAILED.code()))

            recipeEngine!!.executePreInstall(stack, hostGroups)

            val clusterName = cluster.name
            val blueprintName = cluster.blueprint.blueprintName
            val configStrategy = cluster.configStrategy.name
            val clusterTemplate: String
            if (cluster.isSecure) {
                clusterTemplate = ambariClient.createSecureCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                        cluster.password, cluster.kerberosAdmin + PRINCIPAL, cluster.kerberosPassword, KEY_TYPE)
            } else {
                clusterTemplate = ambariClient.createCluster(clusterName, blueprintName, hostGroupMappings, configStrategy, cluster.password)
            }
            LOGGER.info("Submitted cluster creation template: {}", JsonUtil.minify(clusterTemplate))
            var pollingResult = ambariOperationService!!.waitForOperationsToStart(stack, ambariClient, singletonMap("INSTALL_START", 1),
                    START_OPERATION_STATE)
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()))
            pollingResult = waitForClusterInstall(stack, ambariClient)
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()))

            recipeEngine.executePostInstall(stack)

            executeSmokeTest(stack, ambariClient)
            //TODO https://hortonworks.jira.com/browse/BUG-51920
            startStoppedServices(stack, ambariClient, stack.cluster.blueprint.blueprintName)
            triggerSmartSenseCapture(ambariClient, blueprintText)
            cluster = handleClusterCreationSuccess(stack, cluster)
            return cluster
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (hre: HttpResponseException) {
            val errorMessage = AmbariClientExceptionUtil.getErrorMessage(hre)
            throw AmbariOperationFailedException("Ambari could not create the cluster: " + errorMessage, hre)
        } catch (e: Exception) {
            LOGGER.error("Error while building the Ambari cluster. Message {}, throwable: {}", e.message, e)
            throw AmbariOperationFailedException(e.message, e)
        }

    }

    @Throws(IOException::class, CloudbreakImageNotFoundException::class)
    private fun updateBlueprintConfiguration(stack: Stack, blueprintText: String, rdsConfig: RDSConfig?, fs: FileSystem?): String {
        var blueprintText = blueprintText
        if (fs != null) {
            blueprintText = extendBlueprintWithFsConfig(blueprintText, fs, stack)
        }
        blueprintText = smartSenseConfigProvider!!.addToBlueprint(stack, blueprintText)
        val image = imageService!!.getImage(stack.id)
        if (image.hdpVersion != null) {
            blueprintText = blueprintProcessor!!.modifyHdpVersion(blueprintText, image.hdpVersion)
        }
        if (rdsConfig != null) {
            blueprintText = blueprintProcessor!!.addConfigEntries(blueprintText, rdsConfigProvider!!.getConfigs(rdsConfig), true)
            blueprintText = blueprintProcessor.removeComponentFromBlueprint("MYSQL_SERVER", blueprintText)
        }
        return blueprintText
    }

    private fun executeSmokeTest(stack: Stack, ambariClient: AmbariClient) {
        val pollingResult: PollingResult
        pollingResult = runSmokeTest(stack, ambariClient)
        if (isExited(pollingResult)) {
            throw CancellationException("Stack or cluster in delete in progress phase.")
        } else if (isFailure(pollingResult) || isTimeout(pollingResult)) {
            eventService!!.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name,
                    cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_MR_SMOKE_FAILED.code()))
        }
    }

    @Throws(ClusterException::class)
    private fun checkPollingResult(pollingResult: PollingResult, message: String) {
        if (isExited(pollingResult)) {
            throw CancellationException("Stack or cluster in delete in progress phase.")
        } else if (isTimeout(pollingResult) || isFailure(pollingResult)) {
            throw ClusterException(message)
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun waitForAmbariHosts(stack: Stack) {
        val ambariClient = getSecureAmbariClient(stack)
        val hostMetadata = hostMetadataRepository!!.findHostsInCluster(stack.cluster.id)
        waitForHosts(stack, ambariClient, hostMetadata)
    }

    @Throws(CloudbreakException::class)
    fun installServices(stack: Stack, hostGroup: HostGroup, hostMetadata: Set<HostMetadata>) {
        val upscaleHostNames = getHostNames(hostMetadata)
        val ambariClient = getSecureAmbariClient(stack)
        val pollingResult = ambariOperationService!!.waitForOperations(stack, ambariClient,
                installServices(upscaleHostNames, stack, ambariClient, hostGroup.name), UPSCALE_AMBARI_PROGRESS_STATE)
        checkPollingResult(pollingResult, cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_UPSCALE_FAILED.code()))
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun getDefaultAmbariClient(stack: Stack): AmbariClient {
        val cluster = stack.cluster
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        return ambariClientProvider!!.getDefaultAmbariClient(clientConfig, stack.gatewayPort)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun getAmbariClient(stack: Stack): AmbariClient {
        val cluster = stack.cluster
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        return ambariClientProvider!!.getAmbariClient(clientConfig, stack.gatewayPort, cluster.userName, cluster.password)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun getSecureAmbariClient(stack: Stack): AmbariClient {
        val cluster = stack.cluster
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        return ambariClientProvider!!.getSecureAmbariClient(clientConfig, stack.gatewayPort, cluster)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun credentialChangeAmbariCluster(stackId: Long?, newUserName: String, newPassword: String): Cluster {
        val stack = stackRepository!!.findOneWithLists(stackId)
        val cluster = clusterRepository!!.findOneWithLists(stack.cluster.id)
        val oldUserName = cluster.userName
        val oldPassword = cluster.password
        val ambariClient = getSecureAmbariClient(stack)
        if (newUserName == oldUserName) {
            if (newPassword != oldPassword) {
                ambariClient.changePassword(oldUserName, oldPassword, newPassword, true)
            }
        } else {
            ambariClient.createUser(newUserName, newPassword, true)
            ambariClient.deleteUser(oldUserName)
        }
        return cluster
    }

    @Throws(CloudbreakSecuritySetupException::class)
    fun changeOriginalAmbariCredentials(stack: Stack) {
        val cluster = stack.cluster
        LOGGER.info("Changing ambari credentials for cluster: {}, ambari ip: {}", cluster.name, cluster.ambariIp)
        val userName = cluster.userName
        val password = cluster.password
        val ambariClient = getDefaultAmbariClient(stack)
        if (ADMIN == userName) {
            if (ADMIN != password) {
                ambariClient.changePassword(ADMIN, ADMIN, password, true)
            }
        } else {
            ambariClient.createUser(userName, password, true)
            ambariClient.deleteUser(ADMIN)
        }
    }

    @Throws(CloudbreakException::class)
    fun stopCluster(stack: Stack) {
        val ambariClient = getAmbariClient(stack)
        try {
            if (!allServiceStopped(ambariClient.hostComponentsStates)) {
                stopAllServices(stack, ambariClient)
            }
            // TODO: ambari agent containers should be stopped through the orchestrator API
            //            if (!"BYOS".equals(stack.cloudPlatform())) {
            //                stopAmbariAgents(stack, null);
            //            }
        } catch (ex: AmbariConnectionException) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.")
        }

    }

    @Throws(CloudbreakException::class)
    fun startCluster(stack: Stack) {
        val ambariClient = getAmbariClient(stack)
        waitForAmbariToStart(stack)
        if ("BYOS" != stack.cloudPlatform()) {
            startAmbariAgents(stack)
        }
        startAllServices(stack, ambariClient)
    }

    @Throws(CloudbreakException::class)
    fun isAmbariAvailable(stack: Stack): Boolean {
        var result = false
        val cluster = stack.cluster
        if (cluster != null) {
            val ambariClient = getAmbariClient(stack)
            val ambariClientPollerObject = AmbariClientPollerObject(stack, ambariClient)
            try {
                result = ambariHealthCheckerTask!!.checkStatus(ambariClientPollerObject)
            } catch (ex: Exception) {
                result = false
            }

        }
        return result
    }

    @Throws(IOException::class)
    private fun extendBlueprintWithFsConfig(blueprintText: String, fs: FileSystem, stack: Stack): String {
        val fsConfigurator = fileSystemConfigurators!![FileSystemType.valueOf(fs.type)]
        val json = JsonUtil.writeValueAsString(fs.properties)
        val fsConfiguration = JsonUtil.readValue(json, FileSystemType.valueOf(fs.type).clazz) as FileSystemConfiguration
        decorateFsConfigurationProperties(fsConfiguration, stack)
        val resourceProperties = fsConfigurator.createResources(fsConfiguration)
        val bpConfigEntries = fsConfigurator.getFsProperties(fsConfiguration, resourceProperties)
        if (fs.isDefaultFs) {
            bpConfigEntries.addAll(fsConfigurator.getDefaultFsProperties(fsConfiguration))
        }
        return blueprintProcessor!!.addConfigEntries(blueprintText, bpConfigEntries, true)


    }

    private fun decorateFsConfigurationProperties(fsConfiguration: FileSystemConfiguration, stack: Stack) {
        fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stack.id!!)
        if (CloudConstants.AZURE_RM == stack.platformVariant) {
            val resourceGroupName = stack.getResourceByType(ResourceType.ARM_TEMPLATE).resourceName
            fsConfiguration.addProperty(FileSystemConfiguration.RESOURCE_GROUP_NAME, resourceGroupName)
        }
    }

    @Throws(CloudbreakException::class)
    private fun stopAllServices(stack: Stack, ambariClient: AmbariClient) {
        eventService!!.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name,
                cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STOPPING.code()))
        val requestId = ambariClient.stopAllServices()
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to stop on stack")
            val servicesStopResult = ambariOperationService!!.waitForOperations(stack, ambariClient, singletonMap("stop services", requestId),
                    STOP_AMBARI_PROGRESS_STATE)
            if (isExited(servicesStopResult)) {
                throw CancellationException("Cluster was terminated while waiting for Hadoop services to start")
            } else if (isTimeout(servicesStopResult)) {
                throw CloudbreakException("Timeout while stopping Ambari services.")
            }
        } else {
            throw CloudbreakException("Failed to stop Hadoop services.")
        }
        eventService.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name,
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STOPPED.code()))
    }

    @Throws(CloudbreakException::class)
    private fun startAllServices(stack: Stack, ambariClient: AmbariClient) {
        eventService!!.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name,
                cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STARTING.code()))
        val requestId = ambariClient.startAllServices()
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to start on stack")
            val servicesStartResult = ambariOperationService!!.waitForOperations(stack, ambariClient, singletonMap("start services", requestId),
                    START_AMBARI_PROGRESS_STATE)
            if (isExited(servicesStartResult)) {
                throw CancellationException("Cluster was terminated while waiting for Hadoop services to start")
            } else if (isTimeout(servicesStartResult)) {
                throw CloudbreakException("Timeout while starting Ambari services.")
            }
        } else {
            throw CloudbreakException("Failed to start Hadoop services.")
        }
        eventService.fireCloudbreakEvent(stack.id, Status.UPDATE_IN_PROGRESS.name,
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STARTED.code()))
    }

    private fun handleClusterCreationSuccess(stack: Stack, cluster: Cluster): Cluster {
        var cluster = cluster
        LOGGER.info("Cluster created successfully. Cluster name: {}", cluster.name)
        cluster.creationFinished = Date().time
        cluster.upSince = Date().time
        cluster = clusterRepository!!.save(cluster)
        val updatedInstances = ArrayList<InstanceMetaData>()
        for (instanceGroup in stack.instanceGroups) {
            val instances = instanceGroup.allInstanceMetaData
            for (instanceMetaData in instances) {
                if (!instanceMetaData.isTerminated) {
                    instanceMetaData.instanceStatus = InstanceStatus.REGISTERED
                    updatedInstances.add(instanceMetaData)
                }
            }
        }
        instanceMetadataRepository!!.save(updatedInstances)
        val hostMetadata = ArrayList<HostMetadata>()
        for (host in hostMetadataRepository!!.findHostsInCluster(cluster.id)) {
            host.hostMetadataState = HostMetadataState.HEALTHY
            hostMetadata.add(host)
        }
        hostMetadataRepository.save(hostMetadata)
        return cluster

    }

    private fun triggerSmartSenseCapture(ambariClient: AmbariClient, blueprintText: String) {
        if (smartSenseConfigProvider!!.smartSenseIsConfigurable(blueprintText)) {
            try {
                LOGGER.info("Triggering SmartSense data capture.")
                ambariClient.smartSenseCapture(0)
            } catch (e: Exception) {
                LOGGER.error("Triggering SmartSense capture is failed.", e)
            }

        }
    }

    private fun getHostNames(hostMetadata: Set<HostMetadata>): List<String> {
        return hostMetadata.stream().map(Function<HostMetadata, String> { it.getHostName() }).collect(Collectors.toList<String>())
    }

    private fun runSmokeTest(stack: Stack, ambariClient: AmbariClient): PollingResult {
        val id = ambariClient.runMRServiceCheck()
        return ambariOperationService!!.waitForOperations(stack, ambariClient, singletonMap("MR_SMOKE_TEST", id), SMOKE_TEST_AMBARI_PROGRESS_STATE)
    }

    @Throws(CloudbreakException::class)
    private fun waitForAmbariToStart(stack: Stack) {
        LOGGER.info("Checking if Ambari Server is available.")
        val ambariClient = getAmbariClient(stack)
        val ambariHealthCheckResult = ambariHealthChecker!!.pollWithTimeout(
                ambariHealthCheckerTask,
                AmbariClientPollerObject(stack, ambariClient),
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS,
                AmbariOperationService.MAX_FAILURE_COUNT)
        if (isExited(ambariHealthCheckResult)) {
            throw CancellationException("Cluster was terminated while waiting for Ambari to start.")
        } else if (isTimeout(ambariHealthCheckResult)) {
            throw CloudbreakException("Ambari server was not restarted properly.")
        }
    }

    @Throws(CloudbreakException::class)
    private fun startAmbariAgents(stack: Stack) {
        LOGGER.info("Starting Ambari agents on the hosts.")
        val hostsJoinedResult = waitForHostsToJoin(stack)
        if (PollingResult.EXIT == hostsJoinedResult) {
            throw CancellationException("Cluster was terminated while starting Ambari agents.")
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun waitForHostsToJoin(stack: Stack): PollingResult {
        val hostsInCluster = hostMetadataRepository!!.findHostsInCluster(stack.cluster.id)
        val ambariHostsCheckerContext = AmbariHostsCheckerContext(stack, getAmbariClient(stack), hostsInCluster, stack.fullNodeCount!!)
        return ambariHostJoin!!.pollWithTimeout(
                ambariHostsJoinStatusCheckerTask,
                ambariHostsCheckerContext,
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS,
                AmbariOperationService.MAX_FAILURE_COUNT)
    }

    private fun allServiceStopped(hostComponentsStates: Map<String, Map<String, String>>): Boolean {
        var stopped = true
        val values = hostComponentsStates.values
        for (value in values) {
            for (state in value.values) {
                if ("INSTALLED" != state) {
                    stopped = false
                }
            }
        }
        return stopped
    }

    @Throws(CloudbreakException::class)
    private fun startStoppedServices(stack: Stack, ambariClient: AmbariClient, blueprint: String) {
        val components = HashSet<String>()
        val hostComponentsStates = ambariClient.hostComponentsStates
        val values = hostComponentsStates.values
        val componentsCategory = ambariClient.getComponentsCategory(blueprint)
        for (value in values) {
            for (entry in value.entries) {
                val category = componentsCategory[entry.key]
                if ("INSTALLED" == entry.value && "CLIENT" != category) {
                    components.add(entry.key)
                }
            }
        }

        if (!components.isEmpty()) {
            startAllServices(stack, ambariClient)
        }
    }

    @Throws(IOException::class, CloudbreakImageNotFoundException::class)
    private fun setBaseRepoURL(stack: Stack, ambariClient: AmbariClient) {
        val image = imageService!!.getImage(stack.id)
        val hdpRepo = image.hdpRepo
        if (hdpRepo == null) {
            val ambStack = stack.cluster.ambariStackDetails
            if (ambStack != null) {
                LOGGER.info("Use specific Ambari repository: {}", ambStack)
                try {
                    val stackType = ambStack.stack
                    val version = ambStack.version
                    val os = ambStack.os
                    val verify = ambStack.isVerify
                    addRepository(ambariClient, stackType, version, os, ambStack.stackRepoId, ambStack.stackBaseURL, verify)
                    addRepository(ambariClient, stackType, version, os, ambStack.utilsRepoId, ambStack.utilsBaseURL, verify)
                } catch (e: HttpResponseException) {
                    val exceptionErrorMsg = AmbariClientExceptionUtil.getErrorMessage(e)
                    val msg = String.format("Cannot use the specified Ambari stack: %s. Error: %s", ambStack.toString(), exceptionErrorMsg)
                    throw BadRequestException(msg, e)
                }

            } else {
                LOGGER.info("Using latest HDP repository")
            }
        } else {
            val stackRepo = hdpRepo.stack
            val utilRepo = hdpRepo.util
            val stackRepoId = stackRepo.remove(REPO_ID_TAG)
            val utilRepoId = utilRepo.remove(REPO_ID_TAG)
            val typeVersion = stackRepoId.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val stackType = typeVersion[0]
            val version = typeVersion[1]
            for (os in stackRepo.keys) {
                addRepository(ambariClient, stackType, version, os, stackRepoId, stackRepo.get(os), true)
            }
            for (os in utilRepo.keys) {
                addRepository(ambariClient, stackType, version, os, utilRepoId, utilRepo.get(os), true)
            }
        }
    }

    @Throws(HttpResponseException::class)
    private fun addRepository(client: AmbariClient, stack: String, version: String, os: String,
                              repoId: String, repoUrl: String, verify: Boolean) {
        client.addStackRepository(stack, version, os, repoId, repoUrl, verify)
    }

    private fun addBlueprint(stack: Stack, ambariClient: AmbariClient, blueprintText: String) {
        var blueprintText = blueprintText
        try {
            val cluster = stack.cluster
            val hostGroupConfig = hadoopConfigurationService!!.getHostGroupConfiguration(cluster)
            blueprintText = ambariClient.extendBlueprintHostGroupConfiguration(blueprintText, hostGroupConfig)
            val globalConfig = hadoopConfigurationService.getGlobalConfiguration(cluster)
            blueprintText = ambariClient.extendBlueprintGlobalConfiguration(blueprintText, globalConfig)
            if (cluster.isSecure) {
                var gatewayHost = cluster.ambariIp
                if (stack.instanceGroups != null && !stack.instanceGroups.isEmpty()) {
                    val instanceGroupByType = stack.gatewayInstanceGroup
                    gatewayHost = instanceMetadataRepository!!.findAliveInstancesHostNamesInInstanceGroup(instanceGroupByType.id)[0]
                    val domain = gatewayHost.substring(gatewayHost.indexOf(".") + 1)
                    blueprintText = ambariClient.extendBlueprintWithKerberos(blueprintText, gatewayHost, domain.toUpperCase(), domain)
                } else {
                    // TODO this won't work on mesos, but it doesn't work anyway
                    blueprintText = ambariClient.extendBlueprintWithKerberos(blueprintText, gatewayHost, REALM, DOMAIN)
                }
                blueprintText = addHBaseClient(blueprintText)
            }
            LOGGER.info("Adding generated blueprint to Ambari: {}", JsonUtil.minify(blueprintText))
            ambariClient.addBlueprint(blueprintText)
        } catch (e: IOException) {
            if ("Conflict" == e.message) {
                throw BadRequestException("Ambari blueprint already exists.", e)
            } else if (e is HttpResponseException) {
                val errorMessage = AmbariClientExceptionUtil.getErrorMessage(e)
                throw CloudbreakServiceException("Ambari Blueprint could not be added: " + errorMessage, e)
            } else {
                throw CloudbreakServiceException(e)
            }
        }

    }

    // TODO https://issues.apache.org/jira/browse/AMBARI-15295
    private fun addHBaseClient(blueprint: String): String {
        var processingBlueprint = blueprint
        try {
            val root = JsonUtil.readTree(processingBlueprint)
            val hostGroupsNode = root.path("host_groups") as ArrayNode
            val hostGroups = hostGroupsNode.elements()
            while (hostGroups.hasNext()) {
                val hostGroupNode = hostGroups.next()
                val componentsArray = hostGroupNode.path("components") as ArrayNode
                val iterator = componentsArray.elements()
                var masterPresent = false
                var clientPresent = false
                while (iterator.hasNext()) {
                    val componentName = iterator.next().path("name").textValue()
                    if ("HBASE_MASTER" == componentName) {
                        masterPresent = true
                    } else if ("HBASE_CLIENT" == componentName) {
                        clientPresent = true
                    }
                }
                if (masterPresent && !clientPresent) {
                    val arrayElementNode = componentsArray.addObject()
                    arrayElementNode.put("name", "HBASE_CLIENT")
                }
            }
            processingBlueprint = JsonUtil.writeValueAsString(root)
        } catch (e: Exception) {
            LOGGER.warn("Cannot extend blueprint with HBASE_CLIENT", e)
        }

        return processingBlueprint
    }

    private fun extendHiveConfig(ambariClient: AmbariClient, processingBlueprint: String): String {
        val config = HashMap<String, Map<String, String>>()
        val hiveSite = HashMap<String, String>()
        hiveSite.put("hive.server2.authentication.kerberos.keytab", "/etc/security/keytabs/hive2.service.keytab")
        config.put("hive-site", hiveSite)
        return ambariClient.extendBlueprintGlobalConfiguration(processingBlueprint, config)
    }

    private fun waitForHosts(stack: Stack, ambariClient: AmbariClient, hostsInCluster: Set<HostMetadata>): PollingResult {
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.ambariIp)
        return hostsPollingService!!.pollWithTimeoutSingleFailure(
                ambariHostsStatusCheckerTask, AmbariHostsCheckerContext(stack, ambariClient, hostsInCluster, hostsInCluster.size),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS)
    }

    @Throws(InvalidHostGroupHostAssociation::class)
    private fun buildHostGroupAssociations(hostGroups: Set<HostGroup>): Map<String, List<Map<String, String>>> {
        val hostGroupMappings = HashMap<String, List<Map<String, String>>>()
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations")
        for (hostGroup in hostGroups) {
            val hostInfoForHostGroup = ArrayList<Map<String, String>>()
            if (hostGroup.constraint.instanceGroup != null) {
                val topologyMapping = getTopologyMapping(hostGroup)
                val instanceGroupId = hostGroup.constraint.instanceGroup.id
                val metas = instanceMetadataRepository!!.findAliveInstancesInInstanceGroup(instanceGroupId)
                for (meta in metas) {
                    val hostInfo = HashMap<String, String>()
                    hostInfo.put(FQDN, meta.discoveryFQDN)
                    if (meta.hypervisor != null) {
                        hostInfo.put("hypervisor", meta.hypervisor)
                        hostInfo.put("rack", topologyMapping[meta.hypervisor])
                    }
                    hostInfoForHostGroup.add(hostInfo)
                }
            } else {
                for (hostMetadata in hostGroup.hostMetadata) {
                    val hostInfo = HashMap<String, String>()
                    hostInfo.put(FQDN, hostMetadata.hostName)
                    hostInfoForHostGroup.add(hostInfo)
                }
            }

            hostGroupMappings.put(hostGroup.name, hostInfoForHostGroup)
        }
        LOGGER.info("Computed host-hostGroup associations: {}", hostGroupMappings)
        return hostGroupMappings
    }

    private fun getTopologyMapping(hg: HostGroup): Map<String, String> {
        val result = HashMap()
        LOGGER.info("Computing hypervisor - rack mapping based on topology")
        val topology = hg.cluster.stack.credential.topology ?: return result
        val records = topology.records
        if (records != null) {
            for (t in records) {
                result.put(t.hypervisor, t.rack)
            }
        }
        return result
    }

    private fun waitForClusterInstall(stack: Stack, ambariClient: AmbariClient): PollingResult {
        val clusterInstallRequest = HashMap<String, Int>()
        clusterInstallRequest.put("CLUSTER_INSTALL", 1)
        return ambariOperationService!!.waitForOperations(stack, ambariClient, clusterInstallRequest, INSTALL_AMBARI_PROGRESS_STATE)
    }

    private fun installServices(hosts: List<String>, stack: Stack, ambariClient: AmbariClient, hostGroup: String): Map<String, Int> {
        try {
            val cluster = stack.cluster
            val blueprintName = cluster.blueprint.blueprintName
            return singletonMap("UPSCALE_REQUEST", ambariClient.addHostsWithBlueprint(blueprintName, hostGroup, hosts))
        } catch (e: HttpResponseException) {
            if ("Conflict" == e.message) {
                throw BadRequestException("Host already exists.", e)
            } else {
                val errorMessage = AmbariClientExceptionUtil.getErrorMessage(e)
                throw CloudbreakServiceException("Ambari could not install services. " + errorMessage, e)
            }
        }

    }

    private enum class Msg private constructor(private val code: String) {
        AMBARI_CLUSTER_RESETTING_AMBARI_DATABASE("ambari.cluster.resetting.ambari.database"),
        AMBARI_CLUSTER_AMBARI_DATABASE_RESET("ambari.cluster.ambari.database.reset"),
        AMBARI_CLUSTER_RESTARTING_AMBARI_SERVER("ambari.cluster.restarting.ambari.server"),
        AMBARI_CLUSTER_RESTARTING_AMBARI_AGENT("ambari.cluster.restarting.ambari.agent"),
        AMBARI_CLUSTER_AMBARI_AGENT_RESTARTED("ambari.cluster.ambari.agent.restarted"),
        AMBARI_CLUSTER_AMBARI_SERVER_RESTARTED("ambari.cluster.ambari.server.restarted"),
        AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP("ambari.cluster.removing.node.from.hostgroup"),
        AMBARI_CLUSTER_ADDING_NODE_TO_HOSTGROUP("ambari.cluster.adding.node.to.hostgroup"),
        AMBARI_CLUSTER_HOST_JOIN_FAILED("ambari.cluster.host.join.failed"),
        AMBARI_CLUSTER_INSTALL_FAILED("ambari.cluster.install.failed"),
        AMBARI_CLUSTER_UPSCALE_FAILED("ambari.cluster.upscale.failed"),
        AMBARI_CLUSTER_MR_SMOKE_FAILED("ambari.cluster.mr.smoke.failed"),
        AMBARI_CLUSTER_SERVICES_STARTING("ambari.cluster.services.starting"),
        AMBARI_CLUSTER_SERVICES_STARTED("ambari.cluster.services.started"),
        AMBARI_CLUSTER_SERVICES_STOPPING("ambari.cluster.services.stopping"),
        AMBARI_CLUSTER_SERVICES_STOPPED("ambari.cluster.services.stopped");

        fun code(): String {
            return code
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AmbariClusterConnector::class.java)
        private val REALM = "NODE.DC1.CONSUL"
        private val DOMAIN = "node.dc1.consul"
        private val KEY_TYPE = "PERSISTED"
        private val PRINCIPAL = "/admin"
        private val FQDN = "fqdn"
        private val ADMIN = "admin"
        private val REPO_ID_TAG = "repoid"
    }
}
