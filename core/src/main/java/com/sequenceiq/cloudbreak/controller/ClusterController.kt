package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.decorator.Decorator
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class ClusterController : ClusterEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val clusterDecorator: Decorator<Cluster>? = null

    @Autowired
    private val hostGroupDecorator: Decorator<HostGroup>? = null

    @Autowired
    private val clusterService: ClusterService? = null

    @Autowired
    private val hostGroupService: HostGroupService? = null

    @Autowired
    private val blueprintValidator: BlueprintValidator? = null

    @Autowired
    private val fileSystemValidator: FileSystemValidator? = null

    @Autowired
    private val rdsConnectionValidator: RdsConnectionValidator? = null

    @Autowired
    private val stackService: StackService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    private val sssdConfigService: SssdConfigService? = null

    override fun post(stackId: Long?, request: ClusterRequest): Response {
        val user = authenticatedUserService!!.cbUser
        if (request.enableSecurity!! && (request.kerberosMasterKey == null || request.kerberosAdmin == null || request.kerberosPassword == null)) {
            return Response.status(Response.Status.ACCEPTED).build()
        }
        MDCBuilder.buildUserMdcContext(user)
        val stack = stackService!!.getById(stackId)
        if (!stack.isAvailable && CloudConstants.BYOS == stack.cloudPlatform()) {
            throw BadRequestException("Stack is not in 'AVAILABLE' status, cannot create cluster now.")
        }
        fileSystemValidator!!.validateFileSystem(stack.cloudPlatform(), request.fileSystem)
        rdsConnectionValidator!!.validateRdsConnection(request.rdsConfigJson)
        var cluster = conversionService!!.convert<Cluster>(request, Cluster::class.java)
        cluster = clusterDecorator!!.decorate(cluster, stackId, user, request.blueprintId, request.hostGroups, request.validateBlueprint,
                request.sssdConfigId)
        if (cluster.isLdapRequired!! && cluster.sssdConfig == null) {
            cluster.sssdConfig = sssdConfigService!!.getDefaultSssdConfig(user)
        }
        clusterService!!.create(user, stackId, cluster)
        return Response.status(Response.Status.ACCEPTED).build()
    }

    override fun get(stackId: Long?): ClusterResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val stack = stackService!!.get(stackId)
        val cluster = clusterService!!.retrieveClusterForCurrentUser(stackId)
        val clusterJson = clusterService.getClusterJson(stack.ambariIp, stackId)
        return clusterService.getClusterResponse(cluster, clusterJson)
    }

    override fun getPrivate(name: String): ClusterResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val stack = stackService!!.getPrivateStack(name, user)
        val cluster = clusterService!!.retrieveClusterForCurrentUser(stack.id)
        val clusterJson = clusterService.getClusterJson(stack.ambariIp, stack.id)
        return clusterService.getClusterResponse(cluster, clusterJson)
    }

    override fun getPublic(name: String): ClusterResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val stack = stackService!!.getPublicStack(name, user)
        val cluster = clusterService!!.retrieveClusterForCurrentUser(stack.id)
        val clusterJson = clusterService.getClusterJson(stack.ambariIp, stack.id)
        return clusterService.getClusterResponse(cluster, clusterJson)
    }

    @Throws(Exception::class)
    override fun delete(stackId: Long?) {
        val user = authenticatedUserService!!.cbUser
        val stack = stackService!!.get(stackId)
        MDCBuilder.buildMdcContext(stack)
        clusterService!!.delete(user, stackId)
    }

    @Throws(CloudbreakSecuritySetupException::class)
    override fun put(stackId: Long?, updateJson: UpdateClusterJson): Response {
        val stack = stackService!!.get(stackId)
        MDCBuilder.buildMdcContext(stack)
        val userNamePasswordJson = updateJson.userNamePasswordJson
        if (userNamePasswordJson != null) {
            ambariUserNamePasswordChange(stackId, stack, userNamePasswordJson)
            return Response.status(Response.Status.ACCEPTED).build()
        }

        if (updateJson.status != null) {
            LOGGER.info("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.status)
            clusterService!!.updateStatus(stackId, updateJson.status)
            return Response.status(Response.Status.ACCEPTED).build()
        }

        if (updateJson.blueprintId != null && updateJson.hostgroups != null && stack.cluster.isCreateFailed) {
            LOGGER.info("Cluster rebuild request received. Stack id:  {}", stackId)
            recreateCluster(stackId, updateJson)
            return Response.status(Response.Status.ACCEPTED).build()
        }

        if (updateJson.hostGroupAdjustment != null) {
            clusterHostgroupAdjustmentChange(stackId, updateJson, stack)
            return Response.status(Response.Status.ACCEPTED).build()
        }
        LOGGER.error("Invalid cluster update request received. Stack id: {}", stackId)
        throw BadRequestException("Invalid update cluster request!")
    }

    @Throws(CloudbreakSecuritySetupException::class)
    private fun clusterHostgroupAdjustmentChange(stackId: Long?, updateJson: UpdateClusterJson, stack: Stack) {
        if (!stack.isAvailable) {
            throw BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.status))
        }
        LOGGER.info("Cluster host adjustment request received. Stack id: {} ", stackId)
        val blueprint = stack.cluster.blueprint
        val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, updateJson.hostGroupAdjustment.hostGroup) ?: throw BadRequestException(String.format("Host group '%s' not found or not member of the cluster '%s'",
                updateJson.hostGroupAdjustment.hostGroup, stack.name))
        blueprintValidator!!.validateHostGroupScalingRequest(blueprint, hostGroup, updateJson.hostGroupAdjustment.scalingAdjustment)
        clusterService!!.updateHosts(stackId, updateJson.hostGroupAdjustment)
    }

    private fun recreateCluster(stackId: Long?, updateJson: UpdateClusterJson) {
        val user = authenticatedUserService!!.cbUser
        val hostGroups = HashSet<HostGroup>()
        for (json in updateJson.hostgroups) {
            var hostGroup = conversionService!!.convert<HostGroup>(json, HostGroup::class.java)
            hostGroup = hostGroupDecorator!!.decorate(hostGroup, stackId, user, json.constraint, json.recipeIds, false)
            hostGroups.add(hostGroup)
        }
        val stackDetails = updateJson.ambariStackDetails
        var ambariStackDetails: AmbariStackDetails? = null
        if (stackDetails != null) {
            ambariStackDetails = conversionService!!.convert<AmbariStackDetails>(stackDetails, AmbariStackDetails::class.java)
        }
        clusterService!!.recreate(stackId, updateJson.blueprintId, hostGroups, updateJson.validateBlueprint, ambariStackDetails)
    }

    private fun ambariUserNamePasswordChange(stackId: Long?, stack: Stack, userNamePasswordJson: UserNamePasswordJson) {
        if (!stack.isAvailable) {
            throw BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.status))
        }
        if (userNamePasswordJson.oldPassword != stack.cluster.password) {
            throw BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password.", stackId,
                    stack.status))
        }
        LOGGER.info("Cluster username password update request received. Stack id:  {}, username: {}, password: {}",
                stackId, userNamePasswordJson.userName, userNamePasswordJson.password)
        clusterService!!.updateUserNamePassword(stackId, userNamePasswordJson)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterController::class.java)
    }
}
