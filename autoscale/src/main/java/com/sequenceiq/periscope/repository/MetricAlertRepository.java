package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.periscope.domain.MetricAlert;

public interface MetricAlertRepository extends CrudRepository<MetricAlert, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    MetricAlert findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    MetricAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<MetricAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
