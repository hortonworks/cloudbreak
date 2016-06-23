package com.sequenceiq.cloudbreak.service.stack

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway
import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.STOPPED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED

import java.util.Date
import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.google.common.base.Predicate
import com.google.common.collect.Iterables
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.StackValidation
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.image.ImageNameUtil
import com.sequenceiq.cloudbreak.service.image.ImageService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService

@Service
@Transactional
class StackService {

    @Inject
    private val stackRepository: StackRepository? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val imageService: ImageService? = null
    @Inject
    private val clusterRepository: ClusterRepository? = null
    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null
    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null
    @Inject
    private val orchestratorRepository: OrchestratorRepository? = null
    @Inject
    private val tlsSecurityService: TlsSecurityService? = null
    @Inject
    private val terminationService: TerminationService? = null
    @Inject
    private val flowManager: ReactorFlowManager? = null
    @Inject
    private val blueprintValidator: BlueprintValidator? = null
    @Inject
    private val networkConfigurationValidator: NetworkConfigurationValidator? = null
    @Inject
    private val securityRuleRepository: SecurityRuleRepository? = null
    @Inject
    private val eventService: CloudbreakEventService? = null
    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null
    @Inject
    private val connector: ServiceProviderConnectorAdapter? = null
    @Inject
    private val imageNameUtil: ImageNameUtil? = null
    @Inject
    private val containerOrchestratorResolver: ContainerOrchestratorResolver? = null

    @Value("${cb.nginx.port:9443}")
    private val nginxPort: Int = 0

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    fun retrievePrivateStacks(user: CbUser): Set<StackResponse> {
        return convertStacks(stackRepository!!.findForUser(user.userId))
    }

    private fun convertStacks(stacks: Set<Stack>): Set<StackResponse> {
        return conversionService!!.convert(stacks, TypeDescriptor.forObject(stacks),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(StackResponse::class.java))) as Set<StackResponse>
    }

    fun retrieveAccountStacks(user: CbUser): Set<StackResponse> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return convertStacks(stackRepository!!.findAllInAccount(user.account))
        } else {
            return convertStacks(stackRepository!!.findPublicInAccountForUser(user.userId, user.account))
        }
    }

    fun retrieveAccountStacks(account: String): Set<Stack> {
        return stackRepository!!.findAllInAccount(account)
    }

    fun retrieveOwnerStacks(owner: String): Set<Stack> {
        return stackRepository!!.findForUser(owner)
    }

    fun getJsonById(id: Long?): StackResponse {
        val stack = get(id)
        return conversionService!!.convert<StackResponse>(stack, StackResponse::class.java)
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Stack {
        val stack = stackRepository!!.findOne(id) ?: throw NotFoundException(String.format("Stack '%s' not found", id))
        return stack
    }

    fun findLazy(id: Long?): Stack {
        val stack = stackRepository!!.findByIdLazy(id) ?: throw NotFoundException(String.format("Stack '%s' not found", id))
        return stack
    }

    fun getById(id: Long?): Stack {
        val retStack = stackRepository!!.findOneWithLists(id) ?: throw NotFoundException(String.format("Stack '%s' not found", id))
        return retStack
    }

    fun getByIdJson(id: Long?): StackResponse {
        val retStack = stackRepository!!.findOneWithLists(id) ?: throw NotFoundException(String.format("Stack '%s' not found", id))
        return conversionService!!.convert<StackResponse>(retStack, StackResponse::class.java)
    }

    operator fun get(ambariAddress: String): StackResponse {
        val stack = stackRepository!!.findByAmbari(ambariAddress) ?: throw NotFoundException(String.format("Stack not found by Ambari address: '%s' not found", ambariAddress))
        return conversionService!!.convert<StackResponse>(stack, StackResponse::class.java)
    }

    fun getPrivateStack(name: String, cbUser: CbUser): Stack? {
        val stack = stackRepository!!.findByNameInUser(name, cbUser.userId) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        return stack
    }

    fun getPrivateStackJson(name: String, cbUser: CbUser): StackResponse {
        val stack = getPrivateStack(name, cbUser) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        return conversionService!!.convert<StackResponse>(stack, StackResponse::class.java)
    }

    fun getPrivateStackJsonByName(name: String, cbUser: CbUser): StackResponse {
        val stack = stackRepository!!.findByNameInUser(name, cbUser.userId) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        return conversionService!!.convert<StackResponse>(stack, StackResponse::class.java)
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun getPublicStackJsonByName(name: String, cbUser: CbUser): StackResponse {
        val stack = stackRepository!!.findOneByName(name, cbUser.account) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        return conversionService!!.convert<StackResponse>(stack, StackResponse::class.java)
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun getPublicStack(name: String, cbUser: CbUser): Stack {
        val stack = stackRepository!!.findOneByName(name, cbUser.account) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        return stack
    }

    fun delete(name: String, user: CbUser) {
        val stack = stackRepository!!.findByNameInAccount(name, user.account, user.userId) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        delete(stack, user)
    }

    fun forceDelete(name: String, user: CbUser) {
        val stack = stackRepository!!.findByNameInAccount(name, user.account, user.userId) ?: throw NotFoundException(String.format("Stack '%s' not found", name))
        forceDelete(stack, user)
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, stack: Stack, ambariVersion: String, hdpVersion: String): Stack {
        val savedStack: Stack
        stack.owner = user.userId
        stack.account = user.account
        stack.gatewayPort = nginxPort
        setPlatformVariant(stack)
        MDCBuilder.buildMdcContext(stack)
        try {
            if (stack.orchestrator != null) {
                orchestratorRepository!!.save(stack.orchestrator)
            }
            savedStack = stackRepository!!.save(stack)
            MDCBuilder.buildMdcContext(savedStack)
            if ("BYOS" != stack.cloudPlatform()) {
                instanceGroupRepository!!.save(savedStack.instanceGroups)
                tlsSecurityService!!.copyClientKeys(stack.id)
                tlsSecurityService.storeSSHKeys(stack)
                imageService!!.create(savedStack, connector!!.getPlatformParameters(stack), ambariVersion, hdpVersion)
                flowManager!!.triggerProvisioning(savedStack.id)
            } else {
                savedStack.status = Status.AVAILABLE
                savedStack.created = Date().time
                stackRepository.save(savedStack)
            }
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.STACK, stack.name, ex)
        } catch (e: CloudbreakSecuritySetupException) {
            LOGGER.error("Storing of security credentials failed", e)
            throw CloudbreakApiException("Storing security credentials failed", e)
        }

        return savedStack
    }

    private fun setPlatformVariant(stack: Stack) {
        stack.platformVariant = connector!!.checkAndGetPlatformVariant(stack).value()
    }

    fun delete(id: Long?, user: CbUser) {
        val stack = stackRepository!!.findByIdInAccount(id, user.account) ?: throw NotFoundException(String.format("Stack '%s' not found", id))
        delete(stack, user)
    }

    fun forceDelete(id: Long?, user: CbUser) {
        val stack = stackRepository!!.findByIdInAccount(id, user.account) ?: throw NotFoundException(String.format("Stack '%s' not found", id))
        forceDelete(stack, user)
    }

    fun removeInstance(user: CbUser, stackId: Long?, instanceId: String) {
        val stack = get(stackId)
        val instanceMetaData = instanceMetaDataRepository!!.findByInstanceId(stackId, instanceId) ?: throw NotFoundException(String.format("Metadata for instance %s not found.", instanceId))
        if (!stack.isPublicInAccount && stack.owner != user.userId) {
            throw BadRequestException(String.format("Private stack (%s) only modifiable by the owner.", stackId))
        }
        flowManager!!.triggerStackRemoveInstance(stackId, instanceId)
    }

    @Transactional(Transactional.TxType.NEVER)
    fun updateStatus(stackId: Long?, status: StatusRequest) {
        val stack = getById(stackId)
        var cluster: Cluster? = null
        if (stack.cluster != null) {
            cluster = clusterRepository!!.findOneWithLists(stack.cluster.id)
        }
        if ("BYOS" == stack.cloudPlatform()) {
            LOGGER.warn("The status of a 'Bring your own stack' type of infrastructure cannot be changed.")
            return
        }
        when (status) {
            StatusRequest.SYNC -> sync(stack, status, false)
            StatusRequest.FULL_SYNC -> sync(stack, status, true)
            StatusRequest.STOPPED -> stop(stack, cluster, status)
            StatusRequest.STARTED -> start(stack, cluster, status)
            else -> throw BadRequestException("Cannot update the status of stack because status request not valid.")
        }
    }

    private fun sync(stack: Stack, statusRequest: StatusRequest, full: Boolean) {
        if (!stack.isDeleteInProgress && !stack.isStackInDeletionPhase && !stack.isModificationInProgress) {
            if (full) {
                flowManager!!.triggerFullSync(stack.id)
            } else {
                flowManager!!.triggerStackSync(stack.id)
            }
        } else {
            LOGGER.warn("Stack could not be synchronized in {} state!", stack.status)
        }
    }

    private fun stop(stack: Stack, cluster: Cluster?, statusRequest: StatusRequest) {
        if (cluster != null && cluster.isStopInProgress) {
            stackUpdater!!.updateStackStatus(stack.id, STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.")
            val message = cloudbreakMessagesService!!.getMessage(Msg.STACK_STOP_REQUESTED.code())
            eventService!!.fireCloudbreakEvent(stack.id, STOP_REQUESTED.name, message)
        } else {
            if (stack.isStopped) {
                val statusDesc = cloudbreakMessagesService!!.getMessage(Msg.STACK_STOP_IGNORED.code())
                LOGGER.info(statusDesc)
                eventService!!.fireCloudbreakEvent(stack.id, STOPPED.name, statusDesc)
            } else if (stack.infrastructureIsEphemeral()) {
                throw BadRequestException(
                        String.format("Cannot stop a stack if the volumeType is Ephemeral.", stack.id))
            } else if (!stack.isAvailable && !stack.isStopFailed) {
                throw BadRequestException(
                        String.format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.id))
            } else if (cluster != null && !cluster.isStopped && !stack.isStopFailed) {
                throw BadRequestException(
                        String.format("Cannot update the status of stack '%s' to STOPPED, because the cluster is not in STOPPED state.", stack.id))
            } else {
                stackUpdater!!.updateStackStatus(stack.id, STOP_REQUESTED)
                flowManager!!.triggerStackStop(stack.id)
            }
        }
    }

    private fun start(stack: Stack, cluster: Cluster?, statusRequest: StatusRequest) {
        if (stack.isAvailable) {
            val statusDesc = cloudbreakMessagesService!!.getMessage(Msg.STACK_START_IGNORED.code())
            LOGGER.info(statusDesc)
            eventService!!.fireCloudbreakEvent(stack.id, AVAILABLE.name, statusDesc)
        } else if ((!stack.isStopped || cluster != null && !cluster.isStopped) && !stack.isStartFailed) {
            throw BadRequestException(
                    String.format("Cannot update the status of stack '%s' to STARTED, because it isn't in STOPPED state.", stack.id))
        } else if (stack.isStopped || stack.isStartFailed) {
            stackUpdater!!.updateStackStatus(stack.id, START_REQUESTED)
            flowManager!!.triggerStackStart(stack.id)
        }
    }

    fun updateNodeCount(stackId: Long?, instanceGroupAdjustmentJson: InstanceGroupAdjustmentJson) {
        val stack = get(stackId)
        validateStackStatus(stack)
        validateInstanceGroup(stack, instanceGroupAdjustmentJson.instanceGroup)
        validateScalingAdjustment(instanceGroupAdjustmentJson, stack)
        if (instanceGroupAdjustmentJson.withClusterEvent!!) {
            validateHostGroupAdjustment(instanceGroupAdjustmentJson, stack, instanceGroupAdjustmentJson.scalingAdjustment)
        }
        if (instanceGroupAdjustmentJson.scalingAdjustment > 0) {
            stackUpdater!!.updateStackStatus(stackId, UPDATE_REQUESTED)
            flowManager!!.triggerStackUpscale(stack.id, instanceGroupAdjustmentJson)
        } else {
            flowManager!!.triggerStackDownscale(stack.id, instanceGroupAdjustmentJson)
        }
    }

    fun updateMetaDataStatus(id: Long?, hostName: String, status: InstanceStatus): InstanceMetaData {
        val metaData = instanceMetaDataRepository!!.findHostInStack(id, hostName) ?: throw NotFoundException(String.format("Metadata not found on stack:'%s' with hostname: '%s'.", id, hostName))
        metaData.instanceStatus = status
        return instanceMetaDataRepository.save(metaData)
    }

    fun validateStack(stackValidation: StackValidation) {
        networkConfigurationValidator!!.validateNetworkForStack(stackValidation.network, stackValidation.instanceGroups)
        blueprintValidator!!.validateBlueprintForStack(stackValidation.blueprint, stackValidation.hostGroups, stackValidation.instanceGroups)
    }

    fun validateOrchestrator(orchestrator: Orchestrator) {
        try {
            val containerOrchestrator = containerOrchestratorResolver!!.get(orchestrator.type)
            containerOrchestrator?.validateApiEndpoint(OrchestrationCredential(orchestrator.apiEndpoint, orchestrator.attributes.map))
        } catch (e: CloudbreakException) {
            throw BadRequestException(String.format("Invalid orchestrator type: %s", e.message))
        } catch (e: CloudbreakOrchestratorException) {
            throw BadRequestException(String.format("Error occurred when trying to reach orchestrator API: %s", e.message))
        }

    }

    @Transactional(Transactional.TxType.NEVER)
    fun save(stack: Stack): Stack {
        return stackRepository!!.save(stack)
    }

    val allAlive: List<Stack>
        get() = stackRepository!!.findAllAlive()

    private fun validateScalingAdjustment(instanceGroupAdjustmentJson: InstanceGroupAdjustmentJson, stack: Stack) {
        if (0 == instanceGroupAdjustmentJson.scalingAdjustment) {
            throw BadRequestException(String.format("Requested scaling adjustment on stack '%s' is 0. Nothing to do.", stack.id))
        }
        if (0 > instanceGroupAdjustmentJson.scalingAdjustment) {
            val instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupAdjustmentJson.instanceGroup)
            if (-1 * instanceGroupAdjustmentJson.scalingAdjustment!! > instanceGroup.nodeCount) {
                throw BadRequestException(String.format("There are %s instances in instance group '%s'. Cannot remove %s instances.",
                        instanceGroup.nodeCount, instanceGroup.groupName,
                        -1 * instanceGroupAdjustmentJson.scalingAdjustment!!))
            }
            val removableHosts = instanceMetaDataRepository!!.findRemovableInstances(stack.id, instanceGroupAdjustmentJson.instanceGroup).size
            if (removableHosts < -1 * instanceGroupAdjustmentJson.scalingAdjustment!!) {
                throw BadRequestException(
                        String.format("There are %s unregistered instances in instance group '%s' but %s were requested. Decommission nodes from the cluster!",
                                removableHosts, instanceGroup.groupName, instanceGroupAdjustmentJson.scalingAdjustment!! * -1))
            }
        }
    }

    private fun validateHostGroupAdjustment(instanceGroupAdjustmentJson: InstanceGroupAdjustmentJson, stack: Stack, adjustment: Int?) {
        val blueprint = stack.cluster.blueprint
        val hostGroup = Iterables.find(stack.cluster.hostGroups) { input ->
            // TODO: why instancegroups?
            input!!.constraint.instanceGroup.groupName == instanceGroupAdjustmentJson.instanceGroup
        } ?: throw BadRequestException(String.format("Instancegroup '%s' not found or not part of stack '%s'",
                instanceGroupAdjustmentJson.instanceGroup, stack.name))
        blueprintValidator!!.validateHostGroupScalingRequest(blueprint, hostGroup, adjustment)
    }

    private fun validateStackStatus(stack: Stack) {
        if (!stack.isAvailable) {
            throw BadRequestException(String.format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.", stack.id,
                    stack.status))
        }
    }

    private fun validateInstanceGroup(stack: Stack, instanceGroupName: String) {
        val instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName) ?: throw BadRequestException(String.format("Stack '%s' does not have an instanceGroup named '%s'.", stack.id, instanceGroup))
        if (isGateway(instanceGroup.instanceGroupType)) {
            throw BadRequestException("The Ambari server instance group modification is not enabled.")
        }
    }

    private fun delete(stack: Stack, user: CbUser) {
        LOGGER.info("Stack delete requested.")
        if (user.userId != stack.owner && !user.roles.contains(CbUserRole.ADMIN)) {
            throw BadRequestException("Stacks can be deleted only by account admins or owners.")
        }
        if (!stack.isDeleteCompleted) {
            if ("BYOS" != stack.cloudPlatform()) {
                flowManager!!.triggerTermination(stack.id)
            } else {
                terminationService!!.finalizeTermination(stack.id, false)
            }
        } else {
            LOGGER.info("Stack is already deleted.")
        }
    }

    private fun forceDelete(stack: Stack, user: CbUser) {
        LOGGER.info("Stack forced delete requested.")
        if (user.userId != stack.owner && !user.roles.contains(CbUserRole.ADMIN)) {
            throw BadRequestException("Stacks can be force deleted only by account admins or owners.")
        }
        if (!stack.isDeleteCompleted) {
            flowManager!!.triggerForcedTermination(stack.id)
        } else {
            LOGGER.info("Stack is already deleted.")
        }
    }

    private enum class Msg private constructor(private val code: String) {
        STACK_STOP_IGNORED("stack.stop.ignored"),
        STACK_START_IGNORED("stack.start.ignored"),
        STACK_STOP_REQUESTED("stack.stop.requested");

        fun code(): String {
            return code
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StackService::class.java)
    }
}
