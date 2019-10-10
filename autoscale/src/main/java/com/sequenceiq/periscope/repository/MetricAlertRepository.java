package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.repository.BaseRepository;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.MetricAlert;

@HasPermission
@EntityType(entityClass = MetricAlert.class)
public interface MetricAlertRepository extends BaseRepository<MetricAlert, Long> {

    MetricAlert findByCluster(@Param("alertId") Long alertId, @Param("clusterId") Long clusterId);

    @Query("select m from MetricAlert m where m.scalingPolicy <> null and m.cluster.id = :clusterId")
    List<MetricAlert> findAllWithScalingPolicyByCluster(@Param("clusterId") Long clusterId);

    List<MetricAlert> findAllByCluster(@Param("clusterId") Long clusterId);
}
