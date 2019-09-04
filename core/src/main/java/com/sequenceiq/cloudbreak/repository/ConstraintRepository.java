package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Constraint.class)
@Transactional(TxType.REQUIRED)
public interface ConstraintRepository extends CrudRepository<Constraint, Long> {


}
