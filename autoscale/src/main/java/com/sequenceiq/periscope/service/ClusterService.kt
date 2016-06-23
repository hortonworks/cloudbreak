package com.sequenceiq.periscope.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import com.sequenceiq.periscope.api.model.ClusterState
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.PeriscopeUser
import com.sequenceiq.periscope.domain.SecurityConfig
import com.sequenceiq.periscope.model.AmbariStack
import com.sequenceiq.periscope.repository.ClusterRepository
import com.sequenceiq.periscope.repository.SecurityConfigRepository
import com.sequenceiq.periscope.repository.UserRepository

@Service
class ClusterService {

    @Autowired
    private val clusterRepository: ClusterRepository? = null
    @Autowired
    private val userRepository: UserRepository? = null
    @Autowired
    private val securityConfigRepository: SecurityConfigRepository? = null
    @Autowired
    private val alertService: AlertService? = null

    fun create(user: PeriscopeUser, stack: AmbariStack): Cluster {
        val periscopeUser = createUserIfAbsent(user)
        var cluster = Cluster(periscopeUser, stack)
        cluster = save(cluster)
        if (stack.securityConfig != null) {
            val securityConfig = stack.securityConfig
            securityConfig.cluster = cluster
            securityConfigRepository!!.save(securityConfig)
        }
        alertService!!.addPeriscopeAlerts(cluster)
        return cluster
    }

    fun update(clusterId: Long, stack: AmbariStack): Cluster {
        var cluster = findOneByUser(clusterId)
        cluster.update(stack)
        cluster = save(cluster)
        if (stack.securityConfig != null) {
            val updatedConfig = stack.securityConfig
            val securityConfig = securityConfigRepository!!.findByClusterId(clusterId)
            securityConfig.update(updatedConfig)
            securityConfigRepository.save(securityConfig)
        }
        return cluster
    }

    fun findAllByUser(user: PeriscopeUser): List<Cluster> {
        return clusterRepository!!.findAllByUser(user.id)
    }

    fun findOneByUser(clusterId: Long): Cluster {
        return clusterRepository!!.findOne(clusterId)
    }

    fun save(cluster: Cluster): Cluster {
        return clusterRepository!!.save(cluster)
    }

    fun find(clusterId: Long): Cluster {
        return clusterRepository!!.find(clusterId)
    }

    fun remove(clusterId: Long) {
        val cluster = findOneByUser(clusterId)
        clusterRepository!!.delete(cluster)
    }

    fun updateScalingConfiguration(clusterId: Long, scalingConfiguration: ScalingConfigurationJson) {
        val cluster = findOneByUser(clusterId)
        cluster.minSize = scalingConfiguration.minSize
        cluster.maxSize = scalingConfiguration.maxSize
        cluster.coolDown = scalingConfiguration.coolDown
        save(cluster)
    }

    fun getScalingConfiguration(clusterId: Long): ScalingConfigurationJson {
        val cluster = findOneByUser(clusterId)
        val configuration = ScalingConfigurationJson()
        configuration.coolDown = cluster.coolDown
        configuration.maxSize = cluster.maxSize
        configuration.minSize = cluster.minSize
        return configuration
    }

    fun setState(clusterId: Long, state: ClusterState): Cluster {
        val cluster = findOneByUser(clusterId)
        cluster.state = state
        return clusterRepository!!.save(cluster)
    }

    fun findAll(state: ClusterState): List<Cluster> {
        return clusterRepository!!.findAllByState(state)
    }

    private fun createUserIfAbsent(user: PeriscopeUser): PeriscopeUser {
        var periscopeUser: PeriscopeUser? = userRepository!!.findOne(user.id)
        if (periscopeUser == null) {
            periscopeUser = userRepository.save(user)
        }
        return periscopeUser
    }

}
