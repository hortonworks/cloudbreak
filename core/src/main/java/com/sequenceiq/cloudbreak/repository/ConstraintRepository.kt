package com.sequenceiq.cloudbreak.repository

import org.springframework.data.repository.CrudRepository

import com.sequenceiq.cloudbreak.domain.Constraint

@EntityType(entityClass = Constraint::class)
interface ConstraintRepository : CrudRepository<Constraint, Long>
