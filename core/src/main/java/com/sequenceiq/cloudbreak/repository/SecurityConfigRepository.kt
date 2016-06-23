package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.SecurityConfig

@EntityType(entityClass = SecurityConfig::class)
interface SecurityConfigRepository : CrudRepository<SecurityConfig, Long> {

    fun getServerCertByStackId(@Param("stackId") stackId: Long?): String

}
