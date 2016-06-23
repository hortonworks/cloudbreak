package com.sequenceiq.periscope.rest.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import com.sequenceiq.periscope.api.endpoint.PolicyEndpoint
import com.sequenceiq.periscope.api.model.ScalingPolicyJson
import com.sequenceiq.periscope.domain.ScalingPolicy
import com.sequenceiq.periscope.rest.converter.ScalingPolicyConverter
import com.sequenceiq.periscope.service.ScalingService

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
class PolicyController : PolicyEndpoint {

    @Autowired
    private val scalingService: ScalingService? = null
    @Autowired
    private val policyConverter: ScalingPolicyConverter? = null

    override fun addScaling(clusterId: Long?, json: ScalingPolicyJson): ScalingPolicyJson {
        val scalingPolicy = policyConverter!!.convert(json)
        return createScalingPolicyJsonResponse(scalingService!!.createPolicy(clusterId!!, json.alertId, scalingPolicy), HttpStatus.CREATED)
    }

    override fun setScaling(clusterId: Long?, policyId: Long?, scalingPolicy: ScalingPolicyJson): ScalingPolicyJson {
        val policy = policyConverter!!.convert(scalingPolicy)
        return createScalingPolicyJsonResponse(scalingService!!.updatePolicy(clusterId!!, policyId!!, policy), HttpStatus.OK)
    }

    override fun getScaling(clusterId: Long?): List<ScalingPolicyJson> {
        return createScalingPoliciesJsonResponse(scalingService!!.getPolicies(clusterId!!), HttpStatus.OK)
    }

    override fun deletePolicy(clusterId: Long?, policyId: Long?) {
        scalingService!!.deletePolicy(clusterId!!, policyId!!)
    }

    private fun createScalingPoliciesJsonResponse(scalingPolicies: List<ScalingPolicy>, status: HttpStatus): List<ScalingPolicyJson> {
        return policyConverter!!.convertAllToJson(scalingPolicies)
    }

    private fun createScalingPolicyJsonResponse(scalingPolicy: ScalingPolicy, status: HttpStatus): ScalingPolicyJson {
        return policyConverter!!.convert(scalingPolicy)
    }

}
