package com.sequenceiq.periscope.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.periscope.domain.ScalingPolicy

interface ScalingPolicyRepository : CrudRepository<ScalingPolicy, Long> {

    fun findByCluster(@Param("clusterId") clusterId: Long?, @Param("policyId") policyId: Long?): ScalingPolicy

    fun findAllByCluster(@Param("id") id: Long?): List<ScalingPolicy>
}
