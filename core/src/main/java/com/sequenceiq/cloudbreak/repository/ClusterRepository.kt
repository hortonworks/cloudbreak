package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.api.model.Status

@EntityType(entityClass = Cluster::class)
interface ClusterRepository : CrudRepository<Cluster, Long> {

    fun findById(@Param("id") id: Long?): Cluster

    fun findAllClustersByBlueprint(@Param("id") blueprintId: Long?): Set<Cluster>

    fun findAllClustersBySssdConfig(@Param("id") sssdConfigId: Long?): Set<Cluster>

    fun findOneWithLists(@Param("id") id: Long?): Cluster

    fun findByStatuses(@Param("statuses") statuses: List<Status>): List<Cluster>

    fun findByNameInAccount(@Param("name") name: String, @Param("account") account: String): Cluster

    fun findAllClustersForConstraintTemplate(@Param("id") id: Long?): List<Cluster>

}