package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Template

@EntityType(entityClass = Template::class)
interface TemplateRepository : CrudRepository<Template, Long> {

    override fun findOne(@Param("id") id: Long?): Template

    fun findForUser(@Param("user") user: String): Set<Template>

    fun findPublicInAccountForUser(@Param("user") user: String, @Param("account") account: String): Set<Template>

    fun findAllInAccount(@Param("account") account: String): Set<Template>

    fun findOneByName(@Param("name") name: String, @Param("account") account: String): Template

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String, @Param("owner") owner: String): Template

    fun findByIdInAccount(@Param("id") id: Long?, @Param("account") account: String): Template

    fun findByNameInUser(@Param("name") name: String, @Param("owner") owner: String): Template

    fun findAllDefaultInAccount(@Param("account") account: String): Set<Template>

    fun findByTopology(@Param("topologyId") topologyId: Long?): Set<Template>

}