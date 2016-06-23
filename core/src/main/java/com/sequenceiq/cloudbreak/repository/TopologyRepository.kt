package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Topology

@EntityType(entityClass = Topology::class)
interface TopologyRepository : CrudRepository<Topology, Long> {
    fun findAllInAccount(@Param("account") account: String): Set<Topology>
    fun findByIdInAccount(@Param("id") id: Long?, @Param("account") account: String): Topology
}
