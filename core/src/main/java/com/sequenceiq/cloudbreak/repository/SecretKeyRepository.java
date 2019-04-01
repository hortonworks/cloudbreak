package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.SecretKey;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = SecretKey.class)
@DisableHasPermission
public interface SecretKeyRepository extends DisabledBaseRepository<SecretKey, Long> {
}
