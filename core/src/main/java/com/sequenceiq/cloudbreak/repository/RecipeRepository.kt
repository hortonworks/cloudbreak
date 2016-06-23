package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Recipe

@EntityType(entityClass = Recipe::class)
interface RecipeRepository : CrudRepository<Recipe, Long> {

    override fun findOne(@Param("id") id: Long?): Recipe

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String): Recipe

    fun findPublicInAccountForUser(@Param("owner") userId: String, @Param("account") account: String): Set<Recipe>

    fun findAllInAccount(@Param("account") account: String): Set<Recipe>

    fun findForUser(@Param("owner") userId: String): Set<Recipe>

    fun findByNameForUser(@Param("name") name: String, @Param("owner") userId: String): Recipe

}
