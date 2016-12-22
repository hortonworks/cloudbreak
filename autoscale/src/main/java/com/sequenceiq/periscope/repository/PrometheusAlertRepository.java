package com.sequenceiq.periscope.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.periscope.domain.PrometheusAlert;

public interface PrometheusAlertRepository extends CrudRepository<PrometheusAlert, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    PrometheusAlert findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    PrometheusAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    Set<PrometheusAlert> findAllByCluster(@Param("clusterId") Long clusterId);

}
