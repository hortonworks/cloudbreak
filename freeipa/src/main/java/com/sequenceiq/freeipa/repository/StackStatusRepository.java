package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.entity.StackStatus;

@EntityType(entityClass = StackStatus.class)
@Transactional(TxType.REQUIRED)
public interface StackStatusRepository extends CrudRepository<StackStatus, Long> {

    Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId);

}


