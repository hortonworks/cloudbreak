package com.sequenceiq.periscope.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import com.sequenceiq.periscope.domain.BaseAlert
import com.sequenceiq.periscope.domain.ScalingPolicy
import com.sequenceiq.periscope.repository.ScalingPolicyRepository

@Service
class ScalingService {

    @Autowired
    private val clusterService: ClusterService? = null
    @Autowired
    private val alertService: AlertService? = null
    @Autowired
    private val policyRepository: ScalingPolicyRepository? = null

    fun createPolicy(clusterId: Long, alertId: Long, policy: ScalingPolicy): ScalingPolicy {
        val alert = alertService!!.getBaseAlert(clusterId, alertId)
        policy.alert = alert
        val scalingPolicy = policyRepository!!.save(policy)
        alert.scalingPolicy = scalingPolicy
        alertService.save(alert)
        return scalingPolicy
    }

    fun updatePolicy(clusterId: Long, policyId: Long, scalingPolicy: ScalingPolicy): ScalingPolicy {
        val policy = getScalingPolicy(clusterId, policyId)
        policy.name = scalingPolicy.name
        policy.hostGroup = scalingPolicy.hostGroup
        policy.adjustmentType = scalingPolicy.adjustmentType
        policy.scalingAdjustment = scalingPolicy.scalingAdjustment
        return policyRepository!!.save(policy)
    }

    fun deletePolicy(clusterId: Long, policyId: Long) {
        val policy = getScalingPolicy(clusterId, policyId)
        val alert = policy.alert
        alert.scalingPolicy = null
        policy.alert = null
        policyRepository!!.delete(policy)
        alertService!!.save(alert)
    }

    fun getPolicies(clusterId: Long): List<ScalingPolicy> {
        clusterService!!.findOneByUser(clusterId)
        return policyRepository!!.findAllByCluster(clusterId)
    }

    private fun getScalingPolicy(clusterId: Long, policyId: Long): ScalingPolicy {
        val policy = policyRepository!!.findByCluster(clusterId, policyId) ?: throw NotFoundException("Scaling policy not found")
        alertService!!.getBaseAlert(clusterId, policy.alertId)
        return policy
    }

}
