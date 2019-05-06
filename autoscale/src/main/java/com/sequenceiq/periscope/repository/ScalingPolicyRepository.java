package com.sequenceiq.periscope.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.domain.ScalingPolicy;

@DisableHasPermission
@EntityType(entityClass = ScalingPolicy.class)
public interface ScalingPolicyRepository extends DisabledBaseRepository<ScalingPolicy, Long> {

    ScalingPolicy findByCluster(@Param("clusterId") Long clusterId, @Param("policyId") Long policyId);

    List<ScalingPolicy> findAllByCluster(@Param("id") Long id);
}
