package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.repository.BaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.TimeAlert;

@HasPermission
@EntityType(entityClass = TimeAlert.class)
public interface TimeAlertRepository extends BaseRepository<TimeAlert, Long> {

    TimeAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    List<TimeAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
