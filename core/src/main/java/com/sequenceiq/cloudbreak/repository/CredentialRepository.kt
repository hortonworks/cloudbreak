package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.security.access.prepost.PostAuthorize

import com.sequenceiq.cloudbreak.domain.Credential

@EntityType(entityClass = Credential::class)
interface CredentialRepository : CrudRepository<Credential, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    override fun findOne(@Param("id") id: Long?): Credential

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun findOneByName(@Param("name") name: String, @Param("account") account: String): Credential

    fun findForUser(@Param("user") user: String): Set<Credential>

    fun findPublicInAccountForUser(@Param("user") user: String, @Param("account") account: String): Set<Credential>

    fun findAllInAccount(@Param("account") account: String): Set<Credential>

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String, @Param("owner") owner: String): Credential

    fun findByIdInAccount(@Param("id") id: Long?, @Param("account") account: String): Credential

    fun findByNameInUser(@Param("name") name: String, @Param("owner") owner: String): Credential

    fun findByTopology(@Param("topologyId") topologyId: Long?): Set<Credential>

}