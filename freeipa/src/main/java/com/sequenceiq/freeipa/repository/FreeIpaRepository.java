package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@EntityType(entityClass = FreeIpaRepository.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface FreeIpaRepository extends DisabledBaseRepository<FreeIpa, Long> {

    FreeIpa getByStack(Stack stack);

    @Query("SELECT f FROM FreeIpa f WHERE f.stack.id = :stackId")
    FreeIpa getByStackId(@Param("stackId") Long stackId);
}
