package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

@EntityType(entityClass = StackStatus.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface StackStatusRepository extends DisabledBaseRepository<StackStatus, Long> {

    Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId);

}
