package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Orchestrator.class)
@Transactional(TxType.REQUIRED)
public interface OrchestratorRepository extends CrudRepository<Orchestrator, Long> {

    @Query("SELECT o FROM Stack s " +
            "JOIN s.orchestrator o " +
            "WHERE s.id = :stackId")
    Optional<Orchestrator> findByStackId(@Param("stackId") Long stackId);
}
