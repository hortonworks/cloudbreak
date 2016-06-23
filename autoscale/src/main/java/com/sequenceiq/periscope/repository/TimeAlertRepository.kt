package com.sequenceiq.periscope.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.security.access.prepost.PostAuthorize

import com.sequenceiq.periscope.domain.TimeAlert

interface TimeAlertRepository : CrudRepository<TimeAlert, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    override fun findOne(@Param("id") id: Long?): TimeAlert

    @PostAuthorize("hasPermission(returnObject,'read')")
    fun findByCluster(@Param("alertId") alertId: Long?, @Param("clusterId") clusterId: Long?): TimeAlert

    fun findAllByCluster(@Param("clusterId") clusterId: Long?): List<TimeAlert>
}
