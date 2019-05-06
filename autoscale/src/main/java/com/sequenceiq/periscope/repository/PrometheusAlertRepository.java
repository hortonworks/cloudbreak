package com.sequenceiq.periscope.repository;

import java.util.Set;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.periscope.domain.PrometheusAlert;

@HasPermission
@EntityType(entityClass = PrometheusAlert.class)
public interface PrometheusAlertRepository extends BaseRepository<PrometheusAlert, Long> {

    PrometheusAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    Set<PrometheusAlert> findAllByCluster(@Param("clusterId") Long clusterId);

}
