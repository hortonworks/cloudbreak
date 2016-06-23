package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.security.access.prepost.PostAuthorize

import com.sequenceiq.cloudbreak.domain.Network

@EntityType(entityClass = Network::class)
interface NetworkRepository : CrudRepository<Network, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun findOneById(@Param("id") id: Long?): Network

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun findOneByName(@Param("name") name: String, @Param("account") account: String): Network

    fun findByNameForUser(@Param("name") name: String, @Param("owner") userId: String): Network

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String): Network

    fun findByName(@Param("name") name: String): Set<Network>

    fun findForUser(@Param("owner") user: String): Set<Network>

    fun findPublicInAccountForUser(@Param("owner") user: String, @Param("account") account: String): Set<Network>

    fun findAllInAccount(@Param("account") account: String): Set<Network>

    fun findAllDefaultInAccount(@Param("account") account: String): Set<Network>

    fun findByTopology(@Param("topologyId") topologyId: Long?): Set<Network>
}
