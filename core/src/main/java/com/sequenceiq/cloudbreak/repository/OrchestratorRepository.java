package com.sequenceiq.cloudbreak.repository;


import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Orchestrator;

@EntityType(entityClass = Orchestrator.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface OrchestratorRepository extends CrudRepository<Orchestrator, Long> {

}
