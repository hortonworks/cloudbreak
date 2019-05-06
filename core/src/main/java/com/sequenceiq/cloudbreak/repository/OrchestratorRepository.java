package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.Orchestrator;

@EntityType(entityClass = Orchestrator.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface OrchestratorRepository extends DisabledBaseRepository<Orchestrator, Long> {

}
