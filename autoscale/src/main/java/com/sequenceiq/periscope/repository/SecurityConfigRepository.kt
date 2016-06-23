package com.sequenceiq.periscope.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.periscope.domain.SecurityConfig

interface SecurityConfigRepository : CrudRepository<SecurityConfig, Long> {

    fun findByClusterId(@Param("id") id: Long?): SecurityConfig
}
