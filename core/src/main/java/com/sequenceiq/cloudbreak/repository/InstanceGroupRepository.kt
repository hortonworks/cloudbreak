package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.InstanceGroup

@EntityType(entityClass = InstanceGroup::class)
interface InstanceGroupRepository : CrudRepository<InstanceGroup, Long> {

    override fun findOne(@Param("id") id: Long?): InstanceGroup

    fun findOneByGroupNameInStack(@Param("stackId") stackId: Long?, @Param("groupName") groupName: String): InstanceGroup

}