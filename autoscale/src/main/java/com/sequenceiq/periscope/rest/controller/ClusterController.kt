package com.sequenceiq.periscope.rest.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.endpoint.ClusterEndpoint
import com.sequenceiq.periscope.api.model.AmbariJson
import com.sequenceiq.periscope.api.model.ClusterJson
import com.sequenceiq.periscope.api.model.StateJson
import com.sequenceiq.periscope.domain.Ambari
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.PeriscopeUser
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.model.AmbariStack
import com.sequenceiq.periscope.rest.converter.AmbariConverter
import com.sequenceiq.periscope.rest.converter.ClusterConverter
import com.sequenceiq.periscope.service.AuthenticatedUserService
import com.sequenceiq.periscope.service.ClusterService
import com.sequenceiq.periscope.service.security.ClusterSecurityService

@Component
class ClusterController : ClusterEndpoint {

    @Autowired
    private val clusterService: ClusterService? = null
    @Autowired
    private val ambariConverter: AmbariConverter? = null
    @Autowired
    private val clusterConverter: ClusterConverter? = null
    @Autowired
    private val clusterSecurityService: ClusterSecurityService? = null
    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun addCluster(ambariServer: AmbariJson): ClusterJson {
        val user = authenticatedUserService!!.periscopeUser
        MDCBuilder.buildUserMdcContext(user)
        return setCluster(user, ambariServer, null)
    }

    override fun modifyCluster(ambariServer: AmbariJson, clusterId: Long?): ClusterJson {
        val user = authenticatedUserService!!.periscopeUser
        MDCBuilder.buildMdcContext(user, clusterId)
        return setCluster(user, ambariServer, clusterId)
    }

    override fun getClusters(): List<ClusterJson> {
        val user = authenticatedUserService!!.periscopeUser
        MDCBuilder.buildUserMdcContext(user)
        val clusters = clusterService!!.findAllByUser(user)
        return clusterConverter!!.convertAllToJson(clusters)
    }

    override fun getCluster(clusterId: Long?): ClusterJson {
        val user = authenticatedUserService!!.periscopeUser
        MDCBuilder.buildMdcContext(user, clusterId)
        return createClusterJsonResponse(clusterService!!.findOneByUser(clusterId!!))
    }

    override fun deleteCluster(clusterId: Long?) {
        val user = authenticatedUserService!!.periscopeUser
        MDCBuilder.buildMdcContext(user, clusterId)
        clusterService!!.remove(clusterId!!)
    }

    override fun setState(clusterId: Long?, stateJson: StateJson): ClusterJson {
        val user = authenticatedUserService!!.periscopeUser
        MDCBuilder.buildMdcContext(user, clusterId)
        return createClusterJsonResponse(clusterService!!.setState(clusterId!!, stateJson.state))
    }

    private fun createClusterJsonResponse(cluster: Cluster): ClusterJson {
        return clusterConverter!!.convert(cluster)
    }

    private fun setCluster(user: PeriscopeUser, json: AmbariJson, clusterId: Long?): ClusterJson {
        val ambari = ambariConverter!!.convert(json)
        val access = clusterSecurityService!!.hasAccess(user, ambari)
        if (!access) {
            val host = ambari.host
            LOGGER.info("Illegal access to Ambari cluster '{}' from user '{}'", host, user.email)
            throw AccessDeniedException(String.format("Accessing Ambari cluster '%s' is not allowed", host))
        } else {
            val resolvedAmbari = clusterSecurityService.tryResolve(ambari)
            if (clusterId == null) {
                return createClusterJsonResponse(clusterService!!.create(user, resolvedAmbari))
            } else {
                return createClusterJsonResponse(clusterService!!.update(clusterId, resolvedAmbari))
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ClusterController::class.java)
    }

}
