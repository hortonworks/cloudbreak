package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SecurityConfigRepository extends DisabledBaseRepository<SecurityConfig, Long> {

    Optional<SecurityConfig> findOneByStackId(Long stackId);

}
