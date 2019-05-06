package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.entity.Credential;

@EntityType(entityClass = Credential.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface CredentialRepository extends DisabledBaseRepository<Credential, Long> {

}
