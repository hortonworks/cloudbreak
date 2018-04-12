package com.sequenceiq.cloudbreak.repository;


import com.sequenceiq.cloudbreak.domain.Orchestrator;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = Orchestrator.class)
public interface OrchestratorRepository extends CrudRepository<Orchestrator, Long> {

}
