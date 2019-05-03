package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.freeipa.entity.Credential;

@EntityType(entityClass = Credential.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface CredentialRepository extends DisabledBaseRepository<Credential, Long> {

}
