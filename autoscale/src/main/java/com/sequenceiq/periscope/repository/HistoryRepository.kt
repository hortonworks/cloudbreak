package com.sequenceiq.periscope.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.security.access.prepost.PostAuthorize

import com.sequenceiq.periscope.domain.History

interface HistoryRepository : CrudRepository<History, Long> {

    fun findAllByCluster(@Param("id") id: Long?): List<History>

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun findByCluster(@Param("clusterId") clusterId: Long?, @Param("historyId") historyId: Long?): History

}
