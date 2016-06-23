package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.SecurityGroup

@EntityType(entityClass = SecurityGroup::class)
interface SecurityGroupRepository : CrudRepository<SecurityGroup, Long> {

    fun findById(@Param("id") id: Long?): SecurityGroup

    fun findOneById(@Param("id") id: Long?): SecurityGroup

    fun findByNameForUser(@Param("name") name: String, @Param("owner") userId: String): SecurityGroup

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String): SecurityGroup

    fun findByName(@Param("name") name: String): Set<SecurityGroup>

    fun findForUser(@Param("owner") user: String): Set<SecurityGroup>

    fun findPublicInAccountForUser(@Param("owner") user: String, @Param("account") account: String): Set<SecurityGroup>

    fun findAllInAccount(@Param("account") account: String): Set<SecurityGroup>

    fun findAllDefaultInAccount(@Param("account") account: String): Set<SecurityGroup>
}
