package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.entity.StackAuthentication;

@EntityType(entityClass = StackAuthentication.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface StackAuthenticationRepository extends DisabledBaseRepository<StackAuthentication, Long> {
}
