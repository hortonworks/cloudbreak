package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface SecurityConfigRepository extends DisabledBaseRepository<SecurityConfig, Long> {

    SecurityConfig findOneByStackId(Long stackId);

}
