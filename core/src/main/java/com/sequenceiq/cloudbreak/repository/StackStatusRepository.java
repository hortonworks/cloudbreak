package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = StackStatus.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface StackStatusRepository extends DisabledBaseRepository<StackStatus, Long> {

    Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId);

}
