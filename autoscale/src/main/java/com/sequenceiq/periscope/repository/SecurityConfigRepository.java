package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.periscope.domain.SecurityConfig;

@HasPermission
@EntityType(entityClass = SecurityConfig.class)
public interface SecurityConfigRepository extends BaseRepository<SecurityConfig, Long> {

    SecurityConfig findByClusterId(@Param("id") Long id);
}
