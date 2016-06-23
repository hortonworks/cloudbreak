package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.common.type.ComponentType
import com.sequenceiq.cloudbreak.domain.Component

@EntityType(entityClass = Component::class)
interface ComponentRepository : CrudRepository<Component, Long> {

    fun findComponentByStackIdComponentTypeName(@Param("stackId") stackId: Long?, @Param("componentType") componentType: ComponentType,
                                                @Param("name") name: String): Component


}