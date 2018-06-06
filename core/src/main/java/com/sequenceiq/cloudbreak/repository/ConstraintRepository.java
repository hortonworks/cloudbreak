package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Constraint;

@EntityType(entityClass = Constraint.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ConstraintRepository extends CrudRepository<Constraint, Long> {


}
