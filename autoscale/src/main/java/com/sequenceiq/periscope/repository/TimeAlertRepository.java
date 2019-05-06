package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.periscope.domain.TimeAlert;

@HasPermission
@EntityType(entityClass = TimeAlert.class)
public interface TimeAlertRepository extends BaseRepository<TimeAlert, Long> {

    TimeAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<TimeAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
