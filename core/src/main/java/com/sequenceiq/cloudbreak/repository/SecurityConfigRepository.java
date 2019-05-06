package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SecurityConfigRepository extends DisabledBaseRepository<SecurityConfig, Long> {

    Optional<SecurityConfig> findOneByStackId(Long stackId);

}
