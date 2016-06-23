package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.ClusterTemplate

@EntityType(entityClass = ClusterTemplate::class)
interface ClusterTemplateRepository : CrudRepository<ClusterTemplate, Long> {

    override fun findOne(@Param("id") id: Long?): ClusterTemplate

    fun findOneByName(@Param("name") name: String, @Param("account") account: String): ClusterTemplate

    fun findForUser(@Param("user") user: String): Set<ClusterTemplate>

    fun findPublicInAccountForUser(@Param("user") user: String, @Param("account") account: String): Set<ClusterTemplate>

    fun findAllInAccount(@Param("account") account: String): Set<ClusterTemplate>

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String, @Param("owner") owner: String): ClusterTemplate

    fun findByIdInAccount(@Param("id") id: Long?, @Param("account") account: String): ClusterTemplate

    fun findByNameInUser(@Param("name") name: String, @Param("owner") owner: String): ClusterTemplate

    fun findAllDefaultInAccount(@Param("account") account: String): Set<ClusterTemplate>

}