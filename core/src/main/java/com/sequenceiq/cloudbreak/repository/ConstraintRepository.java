package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.Constraint;

@EntityType(entityClass = Constraint.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface ConstraintRepository extends DisabledBaseRepository<Constraint, Long> {


}
