package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.Constraint;
import org.springframework.data.repository.CrudRepository;

@EntityType(entityClass = Constraint.class)
public interface ConstraintRepository extends CrudRepository<Constraint, Long> {


}
