package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.LoadAlert;

@EntityType(entityClass = LoadAlert.class)
public interface LoadAlertRepository extends CrudRepository<LoadAlert, Long> {

    LoadAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);
}
