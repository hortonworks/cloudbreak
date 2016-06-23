package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Container

@EntityType(entityClass = Container::class)
interface ContainerRepository : CrudRepository<Container, Long> {

    fun findContainersInCluster(@Param("clusterId") clusterId: Long?): Set<Container>

}
