package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.common.type.ResourceType

@EntityType(entityClass = Resource::class)
interface ResourceRepository : CrudRepository<Resource, Long> {

    override fun findOne(@Param("id") id: Long?): Resource

    fun findByStackIdAndNameAndType(@Param("stackId") stackId: Long?, @Param("name") name: String, @Param("type") type: ResourceType): Resource

    fun findByStackIdAndResourceNameOrReference(@Param("stackId") stackId: Long?, @Param("resource") resource: String): Resource
}
