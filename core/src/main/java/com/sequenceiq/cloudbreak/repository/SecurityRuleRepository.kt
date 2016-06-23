package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.SecurityRule

@EntityType(entityClass = SecurityRule::class)
interface SecurityRuleRepository : CrudRepository<SecurityRule, Long> {

    fun findAllBySecurityGroupId(@Param("securityGroupId") securityGroupId: Long?): List<SecurityRule>

}
