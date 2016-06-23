package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.domain.Stack

@EntityType(entityClass = Stack::class)
interface StackRepository : CrudRepository<Stack, Long> {

    override fun findOne(@Param("id") id: Long?): Stack

    fun findById(@Param("id") id: Long?): Stack

    fun findByIdLazy(@Param("id") id: Long?): Stack

    fun findByAmbari(@Param("ambariIp") ambariIp: String): Stack

    fun findForUser(@Param("user") user: String): Set<Stack>

    fun findPublicInAccountForUser(@Param("user") user: String, @Param("account") account: String): Set<Stack>

    fun findAllInAccount(@Param("account") account: String): Set<Stack>

    fun findOneWithLists(@Param("id") id: Long?): Stack

    fun findAllStackForTemplate(@Param("id") id: Long?): List<Stack>

    fun findStackForCluster(@Param("id") id: Long?): Stack

    fun findByIdInAccount(@Param("id") id: Long?, @Param("account") account: String): Stack

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String, @Param("owner") owner: String): Stack

    fun findByNameInUser(@Param("name") name: String, @Param("owner") owner: String): Stack

    fun findOneByName(@Param("name") name: String, @Param("account") account: String): Stack

    fun findByCredential(@Param("credentialId") credentialId: Long?): List<Stack>

    fun findAllByNetwork(@Param("networkId") networkId: Long?): List<Stack>

    fun findByIdWithSecurityConfig(@Param("id") id: Long?): Stack

    fun findByIdWithSecurityGroup(@Param("id") id: Long?): Stack

    fun findAllBySecurityGroup(@Param("securityGroupId") securityGroupId: Long?): List<Stack>

    fun findAllAlive(): List<Stack>

    fun findByStatuses(@Param("statuses") statuses: List<Status>): List<Stack>

    fun findStacksWithoutEvents(): Set<Long>
}
