package com.sequenceiq.periscope.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.PrometheusAlert;

@EntityType(entityClass = PrometheusAlert.class)
public interface PrometheusAlertRepository extends CrudRepository<PrometheusAlert, Long> {

    PrometheusAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    Set<PrometheusAlert> findAllByCluster(@Param("clusterId") Long clusterId);

}
