package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Blueprint

@EntityType(entityClass = Blueprint::class)
interface BlueprintRepository : CrudRepository<Blueprint, Long> {

    override fun findOne(@Param("id") id: Long?): Blueprint

    fun findOneByName(@Param("name") name: String, @Param("account") account: String): Blueprint

    fun findForUser(@Param("user") user: String): Set<Blueprint>

    fun findPublicInAccountForUser(@Param("user") user: String, @Param("account") account: String): Set<Blueprint>

    fun findAllInAccount(@Param("account") account: String): Set<Blueprint>

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String, @Param("owner") owner: String): Blueprint

    fun findByIdInAccount(@Param("id") id: Long?, @Param("account") account: String): Blueprint

    fun findByNameInUser(@Param("name") name: String, @Param("owner") owner: String): Blueprint

    fun findAllDefaultInAccount(@Param("account") account: String): Set<Blueprint>

}