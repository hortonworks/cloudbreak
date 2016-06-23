package com.sequenceiq.cloudbreak.service.cluster

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.MARATHON

import java.io.IOException
import java.util.Arrays

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.common.type.HostMetadataState
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.ConstraintRepository
import com.sequenceiq.cloudbreak.repository.FileSystemRepository
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil
import com.sequenceiq.cloudbreak.util.JsonUtil

import groovyx.net.http.HttpResponseException

@Service
@Transactional
class AmbariClusterService : ClusterService {

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val blueprintService: BlueprintService? = null

    @Inject
    private val clusterRepository: ClusterRepository? = null

    @Inject
    private val fileSystemRepository: FileSystemRepository? = null

    @Inject
    private val constraintRepository: ConstraintRepository? = null

    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null

    @Inject
    private val instanceMetadataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val ambariClientProvider: AmbariClientProvider? = null

    @Inject
    private val flowManager: ReactorFlowManager? = null

    @Inject
    private val blueprintValidator: BlueprintValidator? = null

    @Inject
    private val eventService: CloudbreakEventService? = null

    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    @Inject
    private val jsonHelper: JsonHelper? = null

    @Inject
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Inject
    private val clusterTerminationService: ClusterTerminationService? = null

    @Inject
    private val hostGroupService: HostGroupService? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Inject
    private val statusToPollGroupConverter: StatusToPollGroupConverter? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null

    private enum class Msg private constructor(private val code: String) {
        AMBARI_CLUSTER_START_IGNORED("ambari.cluster.start.ignored"),
        AMBARI_CLUSTER_STOP_IGNORED("ambari.cluster.stop.ignored"),
        AMBARI_CLUSTER_HOST_STATUS_UPDATED("ambari.cluster.host.status.updated"),
        AMBARI_CLUSTER_START_REQUESTED("ambari.cluster.start.requested");

        fun code(): String {
            return code
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun create(user: CbUser, stackId: Long?, cluster: Cluster): Cluster {
        var cluster = cluster
        val stack = stackService!!.get(stackId)
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.blueprint.id)
        if (stack.cluster != null) {
            throw BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.cluster.name))
        }
        if (clusterRepository!!.findByNameInAccount(cluster.name, user.account) != null) {
            throw DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.name)
        }
        if (Status.CREATE_FAILED == stack.status) {
            throw BadRequestException("Stack creation failed, cannot create cluster.")
        }
        for (hostGroup in cluster.hostGroups) {
            constraintRepository!!.save(hostGroup.constraint)
        }
        if (cluster.fileSystem != null) {
            fileSystemRepository!!.save(cluster.fileSystem)
        }
        cluster.stack = stack
        cluster.owner = user.userId
        cluster.account = user.account
        stack.cluster = cluster
        try {
            cluster = clusterRepository.save(cluster)
            InMemoryStateStore.putCluster(cluster.id, statusToPollGroupConverter!!.convert(cluster.status))
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.name, ex)
        }

        if (stack.isAvailable) {
            flowManager!!.triggerClusterInstall(stack.id)
        }
        return cluster
    }

    override fun delete(user: CbUser, stackId: Long?) {
        val stack = stackService!!.get(stackId)
        LOGGER.info("Cluster delete requested.")
        if (user.userId != stack.owner && !user.roles.contains(CbUserRole.ADMIN)) {
            throw BadRequestException("Clusters can only be deleted by account admins or owners.")
        }
        if (Status.DELETE_COMPLETED == stack.cluster.status) {
            throw BadRequestException("Clusters is already deleted.")
        }
        flowManager!!.triggerClusterTermination(stackId)
    }

    override fun retrieveClusterByStackId(stackId: Long?): Cluster {
        return stackService!!.findLazy(stackId).cluster
    }

    override fun retrieveClusterForCurrentUser(stackId: Long?): ClusterResponse {
        val stack = stackService!!.get(stackId)
        return conversionService!!.convert<ClusterResponse>(stack.cluster, ClusterResponse::class.java)
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun updateAmbariClientConfig(clusterId: Long?, ambariClientConfig: HttpClientConfig): Cluster {
        var cluster = clusterRepository!!.findById(clusterId)
        cluster.ambariIp = ambariClientConfig.apiAddress
        cluster = clusterRepository.save(cluster)
        LOGGER.info("Updated cluster: [ambariIp: '{}', certDir: '{}'].", ambariClientConfig.apiAddress, ambariClientConfig.certDir)
        return cluster
    }

    override fun updateHostCountWithAdjustment(clusterId: Long?, hostGroupName: String, adjustment: Int?) {
        val hostGroup = hostGroupService!!.getByClusterIdAndName(clusterId, hostGroupName)
        val constraint = hostGroup.constraint
        constraint.hostCount = constraint.hostCount!! + adjustment!!
        constraintRepository!!.save(constraint)
    }

    override fun updateHostMetadata(clusterId: Long?, hostsPerHostGroup: Map<String, List<String>>) {
        for (hostGroupEntry in hostsPerHostGroup.entries) {
            val hostGroup = hostGroupService!!.getByClusterIdAndName(clusterId, hostGroupEntry.key)
            if (hostGroup != null) {
                for (hostName in hostGroupEntry.value) {
                    val hostMetadataEntry = HostMetadata()
                    hostMetadataEntry.hostName = hostName
                    hostMetadataEntry.hostGroup = hostGroup
                    hostMetadataEntry.hostMetadataState = HostMetadataState.CONTAINER_RUNNING
                    hostGroup.hostMetadata.add(hostMetadataEntry)
                }
                hostGroupService.save(hostGroup)
            }
        }
    }

    override fun getClusterJson(ambariIp: String, stackId: Long?): String {
        val stack = stackService!!.get(stackId)
        if (stack.ambariIp == null) {
            throw NotFoundException(String.format("Ambari server is not available for the stack.[id: %s]", stackId))
        }
        val cluster = stack.cluster
        try {
            val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stackId, cluster.ambariIp)
            val ambariClient = ambariClientProvider!!.getAmbariClient(clientConfig, stack.gatewayPort, cluster.userName,
                    cluster.password)
            val clusterJson = ambariClient.clusterAsJson ?: throw BadRequestException(String.format("Cluster response coming from Ambari server was null. [Ambari Server IP: '%s']", ambariIp))
            return clusterJson
        } catch (e: HttpResponseException) {
            if ("Not Found" == e.message) {
                throw NotFoundException("Ambari blueprint not found.", e)
            } else {
                val errorMessage = AmbariClientExceptionUtil.getErrorMessage(e)
                throw CloudbreakServiceException("Could not get Cluster from Ambari as JSON: " + errorMessage, e)
            }
        } catch (se: CloudbreakSecuritySetupException) {
            throw CloudbreakServiceException(se)
        }

    }

    @Throws(CloudbreakSecuritySetupException::class)
    override fun updateHosts(stackId: Long?, hostGroupAdjustment: HostGroupAdjustmentJson) {
        val stack = stackService!!.get(stackId)
        val cluster = stack.cluster ?: throw BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId))
        val downscaleRequest = validateRequest(stack, hostGroupAdjustment)
        if (downscaleRequest) {
            updateClusterStatusByStackId(stackId, UPDATE_REQUESTED)
            flowManager!!.triggerClusterDownscale(stackId, hostGroupAdjustment)
        } else {
            flowManager!!.triggerClusterUpscale(stackId, hostGroupAdjustment)
        }
    }

    override fun updateStatus(stackId: Long?, statusRequest: StatusRequest) {
        val stack = stackService!!.get(stackId)
        val cluster = stack.cluster ?: throw BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId))
        when (statusRequest) {
            StatusRequest.SYNC -> sync(stack)
            StatusRequest.STOPPED -> stop(stack, cluster)
            StatusRequest.STARTED -> start(stack, cluster)
            else -> throw BadRequestException("Cannot update the status of cluster because status request not valid")
        }
    }

    override fun updateUserNamePassword(stackId: Long?, userNamePasswordJson: UserNamePasswordJson): Cluster {
        val stack = stackService!!.get(stackId)
        flowManager!!.triggerClusterCredentialChange(stack.id, userNamePasswordJson.userName, userNamePasswordJson.password)
        return stack.cluster
    }

    private fun sync(stack: Stack) {
        flowManager!!.triggerClusterSync(stack.id)
    }

    private fun start(stack: Stack, cluster: Cluster) {
        if (stack.isStartInProgress) {
            val message = cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_START_REQUESTED.code())
            eventService!!.fireCloudbreakEvent(stack.id, START_REQUESTED.name, message)
            updateClusterStatusByStackId(stack.id, START_REQUESTED)
        } else {
            if (cluster.isAvailable) {
                val statusDesc = cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_START_IGNORED.code())
                LOGGER.info(statusDesc)
                eventService!!.fireCloudbreakEvent(stack.id, stack.status.name, statusDesc)
            } else if (!cluster.isClusterReadyForStart && !cluster.isStartFailed) {
                throw BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because it isn't in STOPPED state.", cluster.id))
            } else if (!stack.isAvailable && !cluster.isStartFailed) {
                throw BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.id))
            } else {
                updateClusterStatusByStackId(stack.id, START_REQUESTED)
                flowManager!!.triggerClusterStart(stack.id)
            }
        }
    }

    private fun stop(stack: Stack, cluster: Cluster) {
        if (cluster.isStopped) {
            val statusDesc = cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_STOP_IGNORED.code())
            LOGGER.info(statusDesc)
            eventService!!.fireCloudbreakEvent(stack.id, stack.status.name, statusDesc)
        } else if (stack.infrastructureIsEphemeral()) {
            throw BadRequestException(
                    String.format("Cannot stop a cluster if the volumeType is Ephemeral.", cluster.id))
        } else if (!cluster.isClusterReadyForStop && !cluster.isStopFailed) {
            throw BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STOPPED, because it isn't in AVAILABLE state.", cluster.id))
        } else if (!stack.isStackReadyForStop && !stack.isStopFailed) {
            throw BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.id))
        } else if (cluster.isAvailable || cluster.isStopFailed) {
            updateClusterStatusByStackId(stack.id, STOP_REQUESTED)
            flowManager!!.triggerClusterStop(stack.id)
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun updateClusterStatusByStackId(stackId: Long?, status: Status, statusReason: String): Cluster {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, status, statusReason)
        var cluster: Cluster? = stackService!!.findLazy(stackId).cluster
        if (cluster != null) {
            cluster.status = status
            cluster.statusReason = statusReason
            cluster = clusterRepository!!.save<Cluster>(cluster)
            InMemoryStateStore.putCluster(cluster!!.id, statusToPollGroupConverter!!.convert(status))
        }
        return cluster
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun updateClusterStatusByStackId(stackId: Long?, status: Status): Cluster {
        return updateClusterStatusByStackId(stackId, status, "")
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun updateCluster(cluster: Cluster): Cluster {
        var cluster = cluster
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.id)
        cluster = clusterRepository!!.save(cluster)
        return cluster
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun updateClusterUsernameAndPassword(cluster: Cluster, userName: String, password: String): Cluster {
        var cluster = cluster
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.id)
        cluster.userName = userName
        cluster.password = password
        cluster = clusterRepository!!.save(cluster)
        return cluster
    }

    @Transactional(Transactional.TxType.NEVER)
    override fun updateClusterMetadata(stackId: Long?): Cluster {
        val stack = stackService!!.findLazy(stackId)
        val cluster = stack.cluster ?: throw BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId))
        try {
            val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stackId, cluster.ambariIp)
            val ambariClient = ambariClientProvider!!.getAmbariClient(clientConfig, stack.gatewayPort, stack.cluster.userName,
                    stack.cluster.password)
            val hosts = hostMetadataRepository!!.findHostsInCluster(stack.cluster.id)
            val hostStatuses = ambariClient.hostStatuses
            for (host in hosts) {
                if (hostStatuses.containsKey(host.hostName)) {
                    val newState = if (HostMetadataState.HEALTHY.name == hostStatuses[host.hostName])
                        HostMetadataState.HEALTHY
                    else
                        HostMetadataState.UNHEALTHY
                    val stateChanged = updateHostMetadataByHostState(stack, host.hostName, newState)
                    if (stateChanged && HostMetadataState.HEALTHY === newState) {
                        updateInstanceMetadataStateToRegistered(stackId, host)
                    }
                }
            }
        } catch (e: CloudbreakSecuritySetupException) {
            throw CloudbreakServiceException(e)
        }

        return cluster
    }

    private fun updateInstanceMetadataStateToRegistered(stackId: Long?, host: HostMetadata) {
        val instanceMetaData = instanceMetaDataRepository!!.findHostInStack(stackId, host.hostName)
        if (instanceMetaData != null) {
            instanceMetaData.instanceStatus = InstanceStatus.REGISTERED
            instanceMetadataRepository!!.save(instanceMetaData)
        }
    }

    override fun recreate(stackId: Long?, blueprintId: Long?, hostGroups: Set<HostGroup>?, validateBlueprint: Boolean, ambariStackDetails: AmbariStackDetails?): Cluster {
        var hostGroups = hostGroups
        if (blueprintId == null || hostGroups == null) {
            throw BadRequestException("Blueprint id and hostGroup assignments can not be null.")
        }
        val blueprint = blueprintService!!.get(blueprintId) ?: throw BadRequestException(String.format("Blueprint not exists with '%s' id.", blueprintId))
        val stack = stackService!!.getById(stackId)
        var cluster: Cluster? = clusterRepository!!.findById(stack.cluster.id) ?: throw BadRequestException(String.format("Cluster not exists on stack with '%s' id.", stackId))
        if (validateBlueprint) {
            blueprintValidator!!.validateBlueprintForStack(blueprint, hostGroups, stack.instanceGroups)
        }

        if (MARATHON == stack.orchestrator.type) {
            clusterTerminationService!!.deleteClusterContainers(cluster.id)
            cluster = clusterRepository.findById(stack.cluster.id)
        }

        hostGroups = hostGroupService!!.saveOrUpdateWithMetadata(hostGroups, cluster)
        cluster!!.blueprint = blueprint
        cluster.hostGroups.clear()
        cluster.hostGroups.addAll(hostGroups)
        if (ambariStackDetails != null) {
            cluster.ambariStackDetails = ambariStackDetails
        }
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.blueprint.id)
        cluster.status = REQUESTED
        cluster.stack = stack
        cluster = clusterRepository.save<Cluster>(cluster)

        try {
            triggerClusterInstall(stack, cluster)
        } catch (e: CloudbreakException) {
            throw CloudbreakServiceException(e)
        }

        return stack.cluster
    }

    @Throws(CloudbreakException::class)
    private fun triggerClusterInstall(stack: Stack, cluster: Cluster) {
        val orchestratorType = orchestratorTypeResolver!!.resolveType(stack.orchestrator.type)
        if (orchestratorType.containerOrchestrator() && cluster.containers.isEmpty()) {
            flowManager!!.triggerClusterInstall(stack.id)
        } else {
            flowManager!!.triggerClusterReInstall(stack.id)
        }
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun validateRequest(stack: Stack, hostGroupAdjustment: HostGroupAdjustmentJson): Boolean {
        val hostGroup = getHostGroup(stack, hostGroupAdjustment)
        val scalingAdjustment = hostGroupAdjustment.scalingAdjustment!!
        val downScale = scalingAdjustment < 0
        if (scalingAdjustment == 0) {
            throw BadRequestException("No scaling adjustments specified. Nothing to do.")
        }
        if (!downScale && hostGroup.constraint.instanceGroup != null) {
            validateUnusedHosts(hostGroup.constraint.instanceGroup, scalingAdjustment)
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustment)
            validateComponentsCategory(stack, hostGroupAdjustment)
            if (hostGroupAdjustment.withStackUpdate!! && hostGroupAdjustment.scalingAdjustment > 0) {
                throw BadRequestException("ScalingAdjustment has to be decommission if you define withStackUpdate = 'true'.")
            }
        }
        return downScale
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun validateComponentsCategory(stack: Stack, hostGroupAdjustment: HostGroupAdjustmentJson) {
        val cluster = stack.cluster
        val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stack.id, cluster.ambariIp)
        val ambariClient = ambariClientProvider!!.getAmbariClient(clientConfig, stack.gatewayPort, cluster.userName, cluster.password)
        val hostGroup = hostGroupAdjustment.hostGroup
        val blueprint = cluster.blueprint
        try {
            val root = JsonUtil.readTree(blueprint.blueprintText)
            val blueprintName = root.path("Blueprints").path("blueprint_name").asText()
            val categories = ambariClient.getComponentsCategory(blueprintName, hostGroup)
            for (component in categories.keys) {
                if (categories[component].equals(MASTER_CATEGORY, ignoreCase = true)) {
                    throw BadRequestException(
                            String.format("Cannot downscale the '%s' hostGroupAdjustment group, because it contains a '%s' component", hostGroup, component))
                }
            }
        } catch (e: IOException) {
            LOGGER.warn("Cannot check the host components category", e)
        }

    }

    private fun validateUnusedHosts(instanceGroup: InstanceGroup, scalingAdjustment: Int) {
        val unusedHostsInInstanceGroup = instanceMetadataRepository!!.findUnusedHostsInInstanceGroup(instanceGroup.id)
        if (unusedHostsInInstanceGroup.size < scalingAdjustment) {
            throw BadRequestException(String.format(
                    "There are %s unregistered instances in instance group '%s'. %s more instances needed to complete this request.",
                    unusedHostsInInstanceGroup.size, instanceGroup.groupName, scalingAdjustment - unusedHostsInInstanceGroup.size))
        }
    }

    private fun validateRegisteredHosts(stack: Stack, hostGroupAdjustment: HostGroupAdjustmentJson) {
        val hostMetadata = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, hostGroupAdjustment.hostGroup).hostMetadata
        if (hostMetadata.size <= -1 * hostGroupAdjustment.scalingAdjustment!!) {
            val errorMessage = String.format("[hostGroup: '%s', current hosts: %s, decommissions requested: %s]",
                    hostGroupAdjustment.hostGroup, hostMetadata.size, -1 * hostGroupAdjustment.scalingAdjustment!!)
            throw BadRequestException(String.format(
                    "The host group must contain at least 1 host after the decommission: %s",
                    errorMessage))
        }
    }

    private fun getHostGroup(stack: Stack, hostGroupAdjustment: HostGroupAdjustmentJson): HostGroup {
        val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, hostGroupAdjustment.hostGroup) ?: throw BadRequestException(String.format(
                "Invalid host group: cluster '%s' does not contain a host group named '%s'.",
                stack.cluster.name, hostGroupAdjustment.hostGroup))
        return hostGroup
    }

    private fun updateHostMetadataByHostState(stack: Stack, hostName: String, newState: HostMetadataState): Boolean {
        var stateChanged = false
        val hostMetadata = hostMetadataRepository!!.findHostInClusterByName(stack.cluster.id, hostName)
        val oldState = hostMetadata.hostMetadataState
        if (oldState != newState) {
            stateChanged = true
            hostMetadata.hostMetadataState = newState
            hostMetadataRepository.save(hostMetadata)
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name,
                    cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_HOST_STATUS_UPDATED.code(), Arrays.asList(hostName, newState.name)))
        }
        return stateChanged
    }

    override fun getClusterResponse(response: ClusterResponse, clusterJson: String): ClusterResponse {
        response.setCluster(jsonHelper!!.createJsonFromString(clusterJson))
        return response
    }

    override fun getById(id: Long?): Cluster {
        val cluster = clusterRepository!!.findOne(id) ?: throw NotFoundException(String.format("Cluster '%s' not found", id))
        return cluster
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariClusterService::class.java)
        private val MASTER_CATEGORY = "MASTER"
    }

}
