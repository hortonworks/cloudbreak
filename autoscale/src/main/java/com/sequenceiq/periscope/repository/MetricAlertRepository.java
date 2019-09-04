package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.MetricAlert;

@EntityType(entityClass = MetricAlert.class)
public interface MetricAlertRepository extends CrudRepository<MetricAlert, Long> {

    MetricAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<MetricAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
