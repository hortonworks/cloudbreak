package com.sequenceiq.periscope.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.periscope.domain.MetricAlert;

public interface MetricAlertRepository extends CrudRepository<MetricAlert, Long> {

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    Optional<MetricAlert> findById(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    MetricAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<MetricAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
