package com.sequenceiq.periscope.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.security.access.prepost.PostAuthorize

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.api.model.ClusterState

interface ClusterRepository : CrudRepository<Cluster, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    override fun findOne(@Param("id") id: Long?): Cluster

    fun find(@Param("id") id: Long?): Cluster

    fun findAllByUser(@Param("id") id: String): List<Cluster>

    fun findAllByState(@Param("state") state: ClusterState): List<Cluster>
}
