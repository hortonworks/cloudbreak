package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.periscope.domain.MetricAlert;

@HasPermission
@EntityType(entityClass = MetricAlert.class)
public interface MetricAlertRepository extends BaseRepository<MetricAlert, Long> {

    MetricAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<MetricAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
