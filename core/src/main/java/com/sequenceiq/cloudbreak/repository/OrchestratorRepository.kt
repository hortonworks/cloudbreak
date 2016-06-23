package com.sequenceiq.cloudbreak.repository


import org.springframework.data.repository.CrudRepository

import com.sequenceiq.cloudbreak.domain.Orchestrator

@EntityType(entityClass = Orchestrator::class)
interface OrchestratorRepository : CrudRepository<Orchestrator, Long>
