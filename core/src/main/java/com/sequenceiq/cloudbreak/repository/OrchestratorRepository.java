package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Orchestrator.class)
@Transactional(TxType.REQUIRED)
public interface OrchestratorRepository extends CrudRepository<Orchestrator, Long> {

}
