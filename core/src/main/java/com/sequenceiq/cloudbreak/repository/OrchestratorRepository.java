package com.sequenceiq.cloudbreak.repository;


import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Orchestrator;

@EntityType(entityClass = Orchestrator.class)
public interface OrchestratorRepository extends CrudRepository<Orchestrator, Long> {

}
