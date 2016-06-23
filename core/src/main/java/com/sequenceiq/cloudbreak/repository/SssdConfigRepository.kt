package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.SssdConfig

@EntityType(entityClass = SssdConfig::class)
interface SssdConfigRepository : CrudRepository<SssdConfig, Long> {

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String): SssdConfig

    fun findPublicInAccountForUser(@Param("owner") userId: String, @Param("account") account: String): Set<SssdConfig>

    fun findAllInAccount(@Param("account") account: String): Set<SssdConfig>

    fun findForUser(@Param("owner") userId: String): Set<SssdConfig>

    fun findByNameForUser(@Param("name") name: String, @Param("owner") userId: String): SssdConfig
}
