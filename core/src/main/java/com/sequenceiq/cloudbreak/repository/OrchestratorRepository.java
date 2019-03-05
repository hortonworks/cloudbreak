package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Orchestrator.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface OrchestratorRepository extends DisabledBaseRepository<Orchestrator, Long> {

}
