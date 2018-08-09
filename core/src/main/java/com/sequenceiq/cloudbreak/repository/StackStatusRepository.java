package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = StackStatus.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface StackStatusRepository extends DisabledBaseRepository<StackStatus, Long> {

    StackStatus findFirstByStackIdOrderByCreatedDesc(long stackId);
}
